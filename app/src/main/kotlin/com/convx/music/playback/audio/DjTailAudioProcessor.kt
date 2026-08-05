/**
 * Convx Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.convx.music.playback.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer

/**
 * The two moves a DJ pulls on the *outgoing* track's tail: a beat loop, and a
 * turntable brake.
 *
 * One processor rather than two because they share the same ring buffer and can
 * never run at the same time — you either loop the outro or you stop it dead.
 * Idle it is a straight buffer copy.
 *
 * Both modes emit exactly as many bytes as they consume, so the audio sink's
 * rate is untouched and there is no way for buffering to run away. That is the
 * whole trick: the loop discards incoming audio while replaying the ring, and
 * the brake reads the ring more slowly than it fills, letting the surplus fall
 * off the back — which is fine, because this only ever runs on a player that is
 * seconds from being released.
 */
@UnstableApi
class DjTailAudioProcessor : BaseAudioProcessor() {

    private enum class Mode { OFF, LOOP_CAPTURE, LOOP_PLAY, TAPE_STOP }

    @Volatile
    private var mode = Mode.OFF

    private var channels = 0
    private var sampleRate = 0

    private var ring = ShortArray(0)
    private var ringFrames = 0
    private var writeFrame = 0

    // Loop state
    private var loopFrames = 0
    private var loopCaptured = 0
    private var loopStartFrame = 0
    private var loopReadOffset = 0
    private var halvingsLeft = 0

    // Tape-stop state
    private var readPosition = 0.0
    private var tapeRate = 1.0
    private var tapeDecayPerFrame = 0.0

    /**
     * Capture [loopMs] of the track and then repeat it. [halvings] shortens the
     * loop by half each time it comes round, which is the familiar build —
     * 4 beats, 2, 1 — rather than a single bar repeating flatly.
     */
    fun startLoop(loopMs: Long, halvings: Int = 0) {
        if (sampleRate <= 0 || channels <= 0) return
        loopFrames = ((loopMs * sampleRate) / 1000L).toInt().coerceIn(1, ringFrames)
        loopCaptured = 0
        loopStartFrame = writeFrame
        loopReadOffset = 0
        halvingsLeft = halvings.coerceAtLeast(0)
        mode = Mode.LOOP_CAPTURE
    }

    /** Slow the track to a standstill over [durationMs], pitch falling with it. */
    fun startTapeStop(durationMs: Long) {
        if (sampleRate <= 0 || channels <= 0) return
        val frames = ((durationMs * sampleRate) / 1000L).coerceAtLeast(1L)
        tapeRate = 1.0
        // Linear rate decay: the pitch drop a listener expects from a turntable
        // losing power, and it reaches exactly zero at the end of the window.
        tapeDecayPerFrame = 1.0 / frames
        readPosition = writeFrame.toDouble()
        mode = Mode.TAPE_STOP
    }

    fun stop() {
        mode = Mode.OFF
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT ||
            inputAudioFormat.channelCount !in 1..2 ||
            inputAudioFormat.sampleRate <= 0
        ) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        channels = inputAudioFormat.channelCount
        sampleRate = inputAudioFormat.sampleRate
        ringFrames = sampleRate * RING_SECONDS
        ring = ShortArray(ringFrames * channels)
        writeFrame = 0
        mode = Mode.OFF
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return
        val output = replaceOutputBuffer(remaining)
        val frameCount = remaining / 2 / channels

        when (mode) {
            Mode.OFF -> {
                output.put(inputBuffer)
                output.flip()
                return
            }
            Mode.LOOP_CAPTURE -> captureAndPass(inputBuffer, output, frameCount)
            Mode.LOOP_PLAY -> playLoop(inputBuffer, output, frameCount)
            Mode.TAPE_STOP -> brake(inputBuffer, output, frameCount)
        }
        output.flip()
    }

    /** Still the real track, but recording it so there is something to loop. */
    private fun captureAndPass(input: ByteBuffer, output: ByteBuffer, frameCount: Int) {
        repeat(frameCount) {
            for (channel in 0 until channels) {
                val sample = input.short
                ring[writeFrame * channels + channel] = sample
                output.putShort(sample)
            }
            writeFrame = (writeFrame + 1) % ringFrames
            if (++loopCaptured >= loopFrames) {
                mode = Mode.LOOP_PLAY
                loopReadOffset = 0
                return
            }
        }
    }

    /**
     * Replaying the captured bar. Incoming audio is drained and thrown away so
     * the decoder keeps moving and the byte counts stay matched — the track
     * carries on underneath, we just stop listening to it.
     */
    private fun playLoop(input: ByteBuffer, output: ByteBuffer, frameCount: Int) {
        repeat(frameCount) {
            repeat(channels) { input.short }
            val frame = (loopStartFrame + loopReadOffset) % ringFrames
            for (channel in 0 until channels) {
                output.putShort(ring[frame * channels + channel])
            }
            if (++loopReadOffset >= loopFrames) {
                loopReadOffset = 0
                if (halvingsLeft > 0 && loopFrames > MIN_LOOP_FRAMES) {
                    loopFrames /= 2
                    halvingsLeft--
                }
            }
        }
    }

    /** Reading the ring at a falling rate: tempo and pitch drop together, which
     *  is what makes it sound like a turntable rather than a fade. */
    private fun brake(input: ByteBuffer, output: ByteBuffer, frameCount: Int) {
        repeat(frameCount) {
            for (channel in 0 until channels) {
                ring[writeFrame * channels + channel] = input.short
            }
            writeFrame = (writeFrame + 1) % ringFrames

            if (tapeRate <= 0.0) {
                repeat(channels) { output.putShort(0) }
                return@repeat
            }

            val base = Math.floor(readPosition).toInt()
            val fraction = readPosition - base
            val frameA = Math.floorMod(base, ringFrames)
            val frameB = (frameA + 1) % ringFrames
            for (channel in 0 until channels) {
                val a = ring[frameA * channels + channel].toDouble()
                val b = ring[frameB * channels + channel].toDouble()
                val interpolated = a + (b - a) * fraction
                output.putShort(interpolated.coerceIn(MIN_SAMPLE, MAX_SAMPLE).toInt().toShort())
            }
            readPosition += tapeRate
            tapeRate = (tapeRate - tapeDecayPerFrame).coerceAtLeast(0.0)
        }
    }

    override fun onFlush() {
        mode = Mode.OFF
        writeFrame = 0
        loopCaptured = 0
        loopReadOffset = 0
        if (ring.isNotEmpty()) ring.fill(0)
    }

    override fun onReset() {
        mode = Mode.OFF
        channels = 0
        sampleRate = 0
        ring = ShortArray(0)
        ringFrames = 0
    }

    companion object {
        private const val RING_SECONDS = 4

        /** Roughly a 1/16 note at 120 BPM — below this a "loop" is just a buzz. */
        private const val MIN_LOOP_FRAMES = 2_000

        private const val MIN_SAMPLE = -32768.0
        private const val MAX_SAMPLE = 32767.0
    }
}
