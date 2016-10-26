package Classes;

import java.util.UUID;

/**
 * Created by FlorentinTh on 10/24/2016.
 */

public class BLEService {
    private UUID uuid;

    public BLEService() {}

    public BLEService(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
}
