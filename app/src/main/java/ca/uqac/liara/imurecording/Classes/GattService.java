package ca.uqac.liara.imurecording.Classes;

import java.util.UUID;

/**
 * Created by FlorentinTh on 10/24/2016.
 */

public class GattService {
    private UUID uuid;

    public GattService() {}

    public GattService(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
}
