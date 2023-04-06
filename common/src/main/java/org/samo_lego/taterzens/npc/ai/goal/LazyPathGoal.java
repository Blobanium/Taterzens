package org.samo_lego.taterzens.npc.ai.goal;


import net.minecraft.entity.ai.goal.MoveToTargetPosGoal;
import net.minecraft.entity.mob.PathAwareEntity;

/**
 * Goal used in {@link org.samo_lego.taterzens.npc.NPCData.Movement#PATH} movement.
 */
public class LazyPathGoal extends DirectPathGoal {

    private final PathAwareEntity mob;
    private int nextStartTick;

    public LazyPathGoal(PathAwareEntity mob, double speed) {
        super(mob, speed);
        this.mob = mob;
    }

    @Override
    public boolean canStart() {
        if (this.nextStartTick > 0) {
            --this.nextStartTick;
            return false;
        }
        this.nextStartTick = this.nextStartTick(this.mob);
        return true;
    }
    private int nextStartTick(PathAwareEntity mob) {
        return MoveToTargetPosGoal.toGoalTicks(200 + mob.getRandom().nextInt(200));
    }
}
