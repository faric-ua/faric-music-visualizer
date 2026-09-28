# v0.5.4 Phone Test

## Interaction latency
- [ ] Open projectM and let PRELOAD reach 3/3.
- [ ] Tap FG 10 times: foreground changes should be effectively immediate.
- [ ] Tap 👍 / 👎 repeatedly: UI/status should update immediately.
- [ ] Tap −: rating applies immediately and next visible preset starts without multi-second UI freeze.
- [ ] Tap NEXT 10 times: button/status remain responsive.
- [ ] While a heavy background preset is loading, FG control still responds independently.

## Mode switching
- [ ] TOP can prepare its queue without freezing the screen.
- [ ] ALL can prepare its queue without freezing the screen.
- [ ] Current visualization continues rendering while a new queue is prepared.
- [ ] NEXT still switches to MANUAL.
- [ ] TOP / ALL restore AUTO.

## Regression
- [ ] Ratings persist.
- [ ] Hidden items stay hidden.
- [ ] Solar flares remain.
- [ ] Screen-off playback remains working.
