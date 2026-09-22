# Robot action energy and timing

Upstream references: server/component/Robot.scala move/turn/setLightColor; Settings.scala delay adjustment; application.conf robot.delays and power.cost. New defaults match old config: move 15 energy, turn 2.5, configured delays 0.4 seconds. Effective move/turn pause is max(0.05, configured - 0.06), matching upstream scheduling adjustment.

Port previously performed move/turn free with no pause. Added power.cost.robotMove/robotTurn and robot.delays.move/turn. Operations reserve energy, reject with nil/not enough energy without modifying the world when empty, and refund on failed movement. Occupied target causes 0.4-second pause; successful callbacks request configured pause. ignorePower bypasses these action costs. Failed move callback now returns nil rather than false. setLightColor now returns masked RGB integer and pauses 0.1 seconds, replacing the incorrect boolean result.

Validation: robot-energy-verified.log, full test/build and 450 required GameTests pass; 2055 unit tests, zero failures/errors. New empty-power test explicitly drains constructor's filled machine buffer (initial RED fixture had wrongly assumed empty, corrected after first implementation run). Existing occupied-pre-event test now captures Context.pause and verifies exact failed-move refund, single successful move/turn charge, unchanged source on rejection, resulting facing and light-color return/pause. Two-step real EEPROM and assembled OpenOS/go tests continue passing.

Artifact SHA256 0D113752DEB82D1A4B1CA2190D2A7A9EC13577F51E35CDD61F51D28E144A0DF9. Not installed/live-tested. Client remains closed; installed Modrinth artifact remains A8582A212D3267D316B894CE8C0580AA31A30C931C52A76E8AA241E9183488EE.

Remaining: animation and afterimage; move already-moving guard; detailed upstream blockContent semantics (liquids/replaceable/protected blocks/entities), failure particle/reason parity and action side mapping; swing/use/place/drop action timing and costs; live screen-3 visual/movement tests; chunk-boundary/reload and Lua execution persistence. No full robot parity claim.
