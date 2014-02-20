import gde.model.serial.SerialPort;
import gde.model.serial.SerialPortDefaultImpl;
import gde.model.serial.TxInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import de.treichels.hott.HoTTSerialPort;
import de.treichels.hott.internal.BaseCommand;

public class SerialTest {
  private static SerialPort     portImpl;
  private static HoTTSerialPort port;

  public static void main(final String[] args) throws Exception {
    final List<String> ports = SerialPortDefaultImpl.getAvailablePorts();
    System.out.println(ports);

    portImpl = new SerialPortDefaultImpl(ports.get(0));
    port = new HoTTSerialPort(portImpl);

    // test(new FastModeStop());
    // test(new ClearScreen());
    // test(new ResetScreen());
    // test(new WriteScreen(0, "test"));
    // test(new CloseScreen());

    // test(new ReadModelInformation(0));
    // test(new ReadModelMemory(4096));

    // test(new ServoPositions());

    // test(new QueryTxInfo());
    // test(new BaseCommand(0x00, 0x11, 0));

    // test(new PrepareListMdl());
    // for (int i = 0; i < 512; i++) {
    // test(new ReadTransmitterMemory(0x800 * i));
    // }

    // mx-16
    // test(new ReadTransmitterMemory(0x202c, 20 + 20 * 9));

    // mc-32
    // test(new ReadTransmitterMemory(0x200d, 80 + 80 * 13));

    // final ModelInfo[] modelInfos = port.getAllModelInfos();
    // for (final ModelInfo info : modelInfos) {
    // if (info.getModelType() == ModelType.Unknown) {
    // continue;
    // }
    //
    // System.out.println(info);
    //
    // final byte[] data1 = port.getModelData(info);
    // final byte[] data2 = port.getModelData(info.getModelNumber());
    //
    // for (int i = 0; i < data1.length; i++) {
    // if (data1[i] != data2[i]) {
    // System.out.printf("mismatch at 0x%04x: 0x%02x <> 0x%02x\n", i,
    // data1[i], data2[i]);
    // }
    // }
    // }

    final TxInfo info = port.getTxInfo();
    System.out.println(info);

    final byte[] data1 = port.readMemory(0, 0x3000);

    final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    while (true) {
      final byte[] data2 = port.readMemory(0, 0x3000);

      for (int i = 0; i < data1.length; i++) {
        if (data1[i] != data2[i]) {
          System.out.printf("mismatch at 0x%04x: 0x%02x <> 0x%02x\n", i, data1[i], data2[i]);
        }
      }

      System.out.println("ok");
      reader.readLine();
    }
  }

  @SuppressWarnings("unused")
  private static void test(final BaseCommand cmd) throws IOException {
    System.out.println(cmd);
    System.out.println(port.doCommand(cmd));
  }
}