package li.cil.oc.api.internal;

import li.cil.oc.api.machine.MachineHost;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public interface Agent extends MachineHost, Rotatable {
    Container equipmentInventory();

    Container mainInventory();

    MultiTank tank();

    int selectedSlot();

    void setSelectedSlot(int index);

    int selectedTank();

    void setSelectedTank(int index);

    Player player();

    String name();

    void setName(String name);

    String ownerName();

    UUID ownerUUID();
}
