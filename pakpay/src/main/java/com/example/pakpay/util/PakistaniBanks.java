package com.example.pakpay.util;

import com.example.pakpay.dto.BankDto;

import java.util.List;
import java.util.Optional;

public final class PakistaniBanks {

    private PakistaniBanks() {}

    public static final List<BankDto> ALL = List.of(
            new BankDto("HBL", "Habib Bank Limited", "hbl", "#008269"),
            new BankDto("UBL", "United Bank Limited", "ubl", "#007DC5"),
            new BankDto("MCB", "MCB Bank Limited", "mcb", "#FFD100"),
            new BankDto("MEEZAN", "Meezan Bank", "meezan", "#006747"),
            new BankDto("ABL", "Allied Bank Limited", "abl", "#E31837"),
            new BankDto("ALFALAH", "Bank Alfalah", "alfalah", "#E2231A"),
            new BankDto("FAYSAL", "Faysal Bank", "faysal", "#003B71"),
            new BankDto("SCB", "Standard Chartered", "scb", "#0473EA"),
            new BankDto("BAH", "Bank Al Habib", "bah", "#00A651"),
            new BankDto("ASKARI", "Askari Bank", "askari", "#006633"),
            new BankDto("JS", "JS Bank", "js", "#003DA5"),
            new BankDto("SONERI", "Soneri Bank", "soneri", "#F7941D"),
            new BankDto("SILK", "Silkbank", "silk", "#6B2C91"),
            new BankDto("SUMMIT", "Summit Bank", "summit", "#1E4D8C"),
            new BankDto("NBP", "National Bank of Pakistan", "nbp", "#006B3F"),
            new BankDto("SINDH", "Sindh Bank", "sindh", "#0054A6"),
            new BankDto("BOP", "Bank of Punjab", "bop", "#00529B"),
            new BankDto("DIB", "Dubai Islamic Bank Pakistan", "dib", "#006341"),
            new BankDto("SAMBA", "Samba Bank", "samba", "#004B87"),
            new BankDto("BIPL", "BankIslami Pakistan", "bipl", "#00A651")
    );

    public static Optional<BankDto> findByCode(String code) {
        if (code == null) return Optional.empty();
        return ALL.stream()
                .filter(b -> b.code().equalsIgnoreCase(code))
                .findFirst();
    }
}
