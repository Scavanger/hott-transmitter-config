import static jssc.SerialPort.BAUDRATE_115200;
import static jssc.SerialPort.DATABITS_8;
import static jssc.SerialPort.FLOWCONTROL_NONE;
import static jssc.SerialPort.PARITY_NONE;
import static jssc.SerialPort.STOPBITS_1;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import de.treichels.hott.HoTTDecoder;
import de.treichels.hott.HoTTSerialPort;
import de.treichels.hott.internal.decoder.BaseDecoder;
import de.treichels.hott.internal.io.HoTTWriter;
import gde.model.BaseModel;
import gde.model.enums.ModelType;
import gde.model.serial.FileInfo;
import gde.model.serial.FileMode;
import gde.model.serial.ModelInfo;
import gde.model.serial.TxInfo;
import gde.util.Util;
import jssc.SerialPort;
import jssc.SerialPortList;

public class SerialTest {
    private static HoTTSerialPort port;

    private static void fileReadWriteTest() throws IOException {
        try {
            port.createDir("/test");
        } catch (final IOException e) {
            // ignore
        }
        port.changeDir("/test");
        ByteArrayInputStream in = new ByteArrayInputStream("Hello, World!".getBytes(BaseDecoder.ISO_8859_1));
        port.writeFile("/test/foo", in, FileMode.Create);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        port.readFile("/test/foo", out);
        String hello = new String(out.toByteArray(), BaseDecoder.ISO_8859_1);
        System.out.println(" read /test/foo: " + hello);

        in = new ByteArrayInputStream("Hola, Mondo!".getBytes(BaseDecoder.ISO_8859_1));
        port.writeFile("/test/foo", in);
        out.reset();
        port.readFile("/test/foo", out);
        hello = new String(out.toByteArray(), BaseDecoder.ISO_8859_1);
        System.out.println(" read /test/foo: " + hello);

        port.deleteFile("/test/foo");
        port.deleteFile("/test");
    }

    private static void list(final String path) throws IOException {
        final List<String> subdirs = new ArrayList<>();
        System.out.printf("\nContents of %s:\n", path);

        for (final String name : port.listDir(path))
            try {
                final FileInfo info = port.getFileInfo(name);
                System.out.printf("%1$s %2$8d %3$tF %3$tT %4$s\n", info.getType().toString().substring(0, 1), info.getSize(), info.getModifyDate(),
                        info.getName());

                switch (info.getType()) {
                case Dir:
                    subdirs.add(name);
                    break;

                case File:
                    final String fileName = info.getName();
                    if (fileName.endsWith(".mdl")) {
                        final ModelType type = ModelType.forChar(fileName.charAt(0));
                        final String modelName = fileName.substring(1, fileName.length() - 4);

                        final ByteArrayOutputStream out = new ByteArrayOutputStream();
                        port.readFile(name, out);
                        final ByteArrayInputStream is = new ByteArrayInputStream(out.toByteArray());
                        final BaseModel model = HoTTDecoder.decodeStream(type, modelName, is);
                        if (model.isBound())
                            System.out.printf("TransmitterID: %#x, ModelName: %s, ReceiverID: %#x\n", model.getTransmitterId(), model.getModelName(),
                                    model.getReceiver()[0].getRfid());
                        else
                            System.out.printf("TransmitterID: %#x, ModelName: %s, unbound\n", model.getTransmitterId(), model.getModelName());
                    }
                }
            } catch (final IOException e) {
                System.err.println(name);
                e.printStackTrace();
                continue;
            }

        for (final String subdir : subdirs)
            list(subdir);
    }

    public static void main(final String[] args) throws Exception {
        Stream.of(SerialPortList.getPortNames()).forEach(System.out::println);
        final SerialPort port = new jssc.SerialPort("COM5");
        System.out.println(port.openPort());
        System.out.println(port.setParams(BAUDRATE_115200, DATABITS_8, STOPBITS_1, PARITY_NONE, false, false));
        System.out.println(port.setFlowControlMode(FLOWCONTROL_NONE));
        System.out.println(port.getInputBufferBytesCount());
        System.out.println(port.getOutputBufferBytesCount());

        final HoTTWriter w = new HoTTWriter();
        w.writeUnsignedByte(0);
        w.writeUnsignedByte(1);
        w.writeUnsignedByte(1 ^ 0xFF);
        w.resetCRC();
        w.writeUnsignedShort(0);
        w.writeUnsignedByte(0);
        w.writeUnsignedByte(0x11);
        w.writeUnsignedByte(w.getCRC());

        final byte[] request = w.getData();
        System.out.println(Util.dumpData(request));
        port.writeBytes(request);
        final byte[] response = port.readBytes();
        System.out.println(Util.dumpData(response));

        System.out.println(port.closePort());
        // final List<String> ports = RXTXSerialPort.getAvailablePorts();
        // System.out.println(ports);

        // portImpl = new RXTXSerialPort(ports.get(0));
        // port = new HoTTSerialPort(portImpl);

        // port.open();

        // writeScreenTest();
        // readModelsFromMemoryTest();
        // transmitterInfoTest();
        // readTransmitterMemoryTest();
        // fileReadWriteTest();
        // list("/");

        // for (final String name : port.listDir("/MP3")) {
        // System.out.println(name);
        // }
    }

    private static void readModelsFromMemoryTest() throws IOException {
        final ModelInfo[] modelInfos = port.getAllModelInfos();
        for (final ModelInfo info : modelInfos) {
            if (info.getModelType() == ModelType.Unknown) continue;

            System.out.println(info);

            final byte[] data = port.getModelData(info);
            final ByteArrayInputStream is = new ByteArrayInputStream(data);
            final BaseModel model = HoTTDecoder.decodeStream(info.getModelType(), info.getModelName(), is);
            if (model.isBound())
                System.out.printf("TransmitterID: %#x, ModelName: %s, ReceiverID: %#x\n", model.getTransmitterId(), model.getModelName(),
                        model.getReceiver()[0].getRfid());
            else
                System.out.printf("TransmitterID: %#x, ModelName: %s, unbound\n", model.getTransmitterId(), model.getModelName());
        }
    }

    private static void readTransmitterMemoryTest() throws IOException {
        final byte[] data = port.readMemoryBlock(0, 0x48);
        System.out.println(Util.dumpData(data));
    }

    private static void transmitterInfoTest() throws IOException {
        final TxInfo info = port.getTxInfo();
        System.out.println(info);
    }

    private static void writeScreenTest() throws IOException, InterruptedException {
        port.writeScreen("Test", "1234567890123456789012345678901234567890abcde");
        Thread.sleep(3000);
        port.closeScreen();
    }
}
