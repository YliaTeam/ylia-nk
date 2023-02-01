package cn.nukkit.console;

import cn.nukkit.InterruptibleThread;
import cn.nukkit.Server;
import cn.nukkit.event.server.ServerCommandEvent;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * author: MagicDroidX
 * Nukkit
 */
public class ConsoleCommandReader extends Thread implements InterruptibleThread {

    public static ConsoleCommandReader instance;
    private final LineReader reader;

    private boolean running = true;

    public static ConsoleCommandReader get() {
        return instance;
    }

    public ConsoleCommandReader() {
        if (get() != null) {
            throw new RuntimeException("Command Reader is already exist");
        }

        try {
            var terminal = TerminalBuilder.builder()
                    .name("Nukkit Virtual Terminal")
                    .encoding(StandardCharsets.UTF_8)
                    .jansi(true)
                    .build();

            this.reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .build();

            ConsoleCommandReader.instance = this;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.setName("Nukkit Console Thread");
    }

    public String readLine() {
        try {
            return this.reader.readLine().trim();
        } catch (Exception exception) {
            return "";
        }
    }

    public void redrawBuffer() {
        try {
            this.reader.callWidget(LineReader.REDRAW_LINE);
            this.reader.callWidget(LineReader.REDISPLAY);
            this.reader.getTerminal().flush();
        } catch (Exception ignore) {
        }
    }

    public void run() {
        Server server = Server.getInstance();

        while (this.running) {
            if (Objects.isNull(server.getConsoleSender()) || Objects.isNull(server.getPluginManager())) {
                continue;
            }

            String line = this.readLine();

            if (line == null || line.trim().equals(""))
                continue;

            try {
                ServerCommandEvent event = new ServerCommandEvent(server.getConsoleSender(), line);
                server.getPluginManager().callEvent(event);

                if (!(event.isCancelled())) {
                    server.dispatchCommand(event.getSender(), event.getCommand());
                }
            } catch (Exception e) {
                server.getLogger().logException(e);
            }

            this.redrawBuffer();
        }
    }

    public void shutdown() {
        this.running = false;
    }

    public void clearTerminal() {
        try {
            this.reader.callWidget(LineReader.CLEAR);
        } catch (Exception ignore) {}
    }
}
