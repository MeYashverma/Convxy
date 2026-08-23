# Full YouTube Video Playback

Watch a song's actual YouTube video inside the player instead of audio-only.

## How it works

1. **Toggle** — the video button in the player's action row (next to
   download/like) flips the `WatchVideoKey` preference. Default: off.
2. **Stream resolution** — `MusicService` watches the preference. On change
   it clears the current song's cached stream (both cache namespaces) and
   re-resolves the media item at the same position, passing
   `videoMode = true` to `YTPlayerUtils.playerResponseForPlayback`.
3. **Muxed format selection** — in video mode the resolver skips the
   audio-only intercepts (8spine modules, TIDAL, JioSaavn) and picks the
   best **progressive (muxed audio+video) format** via
   `selectMuxedVideoFormat` (`utils/VideoFormatSelector.kt`): ranked by
   resolution, then fps, then bitrate, capped at 1080p. Muxed keeps playback
   on a single URL so the existing cache/data-source pipeline and ExoPlayer
   setup work unchanged. If the current client exposes no muxed format, the
   resolver falls back to the normal audio format — the toggle stays on but
   nothing breaks; the next song tries video again.
4. **Cache namespacing** — video streams cache under `<mediaId>#video` so
   muxed bytes never collide with the audio cache (same pattern as the
   existing `#flac` namespace).
5. **Rendering** — `VideoPlaybackSurface` (ui/player) attaches a
   `TextureView` to the shared service player (same pattern as
   `CanvasArtworkPlayer`) inside an aspect-ratio-fitting frame. The
   `Thumbnail` composable swaps the artwork carousel for the video surface
   when the toggle is on **and** the persisted format for the current song
   is a video mime type — so the swap only happens once a video stream is
   actually playing, never for a song that silently fell back to audio.

## Notes

- Downloads are unaffected: the download pipeline never passes `videoMode`,
  offline copies stay audio-only.
- Toggling mid-song keeps position (same reload mechanics as an audio
  quality change).
- Unit tests: `VideoFormatSelectorTest` pins the muxed ranking rules.
