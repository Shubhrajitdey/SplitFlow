package com.splitflow.domain.port.in;

import java.util.UUID;

public interface DisputeSplitUseCase {
    void disputeSplit(UUID splitId, String debtorPhone, String reason);
}
