import assert from "node:assert/strict";
import test from "node:test";
import { previewStates, stateFor } from "./dashboard-state.js";

test("defaults to the clearly synthetic preview state", () => {
  assert.equal(stateFor("unknown"), previewStates.preview);
  assert.match(stateFor("preview").eyebrow, /not from your phone/);
});

test("empty and unavailable states do not make inactivity or sleep claims", () => {
  for (const state of [stateFor("empty"), stateFor("unavailable")]) {
    assert.equal(state.apps.length, 0);
    assert.equal(state.timeline.length, 0);
    assert.doesNotMatch(`${state.title} ${state.statusDetail}`, /confirmed sleep|confirmed inactivity|was inactive/i);
  }
});
