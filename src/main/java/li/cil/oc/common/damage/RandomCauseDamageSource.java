package li.cil.oc.common.damage;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

public final class RandomCauseDamageSource extends DamageSource {
    private final String baseMessageId;
    private final int causeCount;

    public RandomCauseDamageSource(final Holder<DamageType> type, final String baseMessageId, final int causeCount) {
        super(type);
        if (causeCount <= 0) {
            throw new IllegalArgumentException("causeCount must be positive");
        }
        this.baseMessageId = baseMessageId;
        this.causeCount = causeCount;
    }

    @Override
    public Component getLocalizedDeathMessage(final LivingEntity livingEntity) {
        return Component.translatable(translationKey(baseMessageId, livingEntity.level().random, causeCount), livingEntity.getDisplayName());
    }

    static String translationKey(final String baseMessageId, final RandomSource random, final int causeCount) {
        if (causeCount <= 0) {
            throw new IllegalArgumentException("causeCount must be positive");
        }
        return "death.attack." + baseMessageId + "." + (random.nextInt(causeCount) + 1);
    }
}
