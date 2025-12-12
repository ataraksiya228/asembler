import java.io.*;
import java.nio.file.*;
import java.util.*;

public class AsmVm {
    static final int MEMORY_SIZE = 1024; // увеличим память

    public static void main(String[] args) throws Exception {
        if (args.length < 1) { usage(); return; }
        String cmd = args[0];
        if ("assemble".equals(cmd)) {
            if (args.length != 3) { usage(); return; }
            assemble(Paths.get(args[1]), Paths.get(args[2]));
        } else {
            usage();
        }
    }

    static void usage() {
        System.out.println("AsmVm — assembler + interpreter");
        System.out.println("Usage:");
        System.out.println("  java AsmVm assemble input.txt output.bin");
        System.out.println("  java AsmVm run output.bin");
        System.out.println("  java AsmVm run output.bin [dump.csv]");
    }

    // ---------- Assembler ----------
    static void assemble(Path in, Path out) throws IOException {
        List<String> lines = Files.readAllLines(in);
        List<byte[]> encoded = new ArrayList<>();
        int lineNo = 0;

        for (String raw : lines) {
            lineNo++;
            String s = raw.trim();
            if (s.isEmpty() || s.startsWith(";")) continue;
            int cpos = s.indexOf(';');
            if (cpos >= 0) s = s.substring(0, cpos).trim();
            if (s.isEmpty()) continue;

            String[] tok = s.split("\\s+");
            String op = tok[0].toLowerCase(Locale.ROOT);
            try {
                long word = 0L;
                switch (op) {
                    case "load": {
                        if (tok.length != 3) throw new IllegalArgumentException("load dest const");
                        int addr = Integer.parseInt(tok[1]);
                        int constant = Integer.parseInt(tok[2]);
                        word |= (2L & 0xF); // opcode 2
                        word |= ((long)addr & 0x3F) << 4; // bits 4-9
                        word |= ((long)constant & 0x7FF) << 10; // bits 10-20
                        break;
                    }
                    case "read": {
                        if (tok.length != 4) throw new IllegalArgumentException("read dest src offset");
                        int addr1 = Integer.parseInt(tok[1]);
                        int addr2 = Integer.parseInt(tok[2]);
                        int offset = Integer.parseInt(tok[3]);
                        word |= (11L & 0xF); // opcode 11
                        word |= ((long)addr1 & 0x3F) << 4; // bits 4-9
                        word |= ((long)addr2 & 0x3F) << 10; // bits 10-15
                        word |= ((long)offset & 0x7FF) << 16; // bits 16-26
                        break;
                    }
                    case "write": {
                        if (tok.length != 4) throw new IllegalArgumentException("write dest src offset");
                        int addr1 = Integer.parseInt(tok[1]);
                        int addr2 = Integer.parseInt(tok[2]);
                        int offset = Integer.parseInt(tok[3]);
                        word |= (3L & 0xF); // opcode 3
                        word |= ((long)addr1 & 0x3F) << 4;
                        word |= ((long)addr2 & 0x3F) << 10;
                        word |= ((long)offset & 0x7FF) << 16;
                        break;
                    }
                    default:
                        throw new IllegalArgumentException("Unknown opcode: " + op);
                }
                encoded.add(to5BytesLE(word));
            } catch (Exception ex) {
                throw new IOException("Error on line " + lineNo + ": " + ex.getMessage(), ex);
            }
        }

        try (OutputStream os = Files.newOutputStream(out)) {
            for (byte[] b : encoded) os.write(b);
        }
        System.out.println("Assembled " + encoded.size() + " instructions to " + out);
    }

    static byte[] to5BytesLE(long word) {
        byte[] b = new byte[5];
        for (int i = 0; i < 5; i++) {
            b[i] = (byte)((word >> (8*i)) & 0xFF);
        }
        return b;
    }
}