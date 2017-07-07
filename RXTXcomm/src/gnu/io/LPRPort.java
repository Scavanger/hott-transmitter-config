/*-------------------------------------------------------------------------
|   RXTX License v 2.1 - LGPL v 2.1 + Linking Over Controlled Interface.
|   RXTX is a native interface to serial ports in java.
|   Copyright 1997-2007 by Trent Jarvi tjarvi@qbang.org and others who
|   actually wrote it.  See individual source files for more information.
|
|   A copy of the LGPL v 2.1 may be found at
|   http://www.gnu.org/licenses/lgpl.txt on March 4th 2007.  A copy is
|   here for your convenience.
|
|   This library is free software; you can redistribute it and/or
|   modify it under the terms of the GNU Lesser General Public
|   License as published by the Free Software Foundation; either
|   version 2.1 of the License, or (at your option) any later version.
|
|   This library is distributed in the hope that it will be useful,
|   but WITHOUT ANY WARRANTY; without even the implied warranty of
|   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
|   Lesser General Public License for more details.
|
|   An executable that contains no derivative of any portion of RXTX, but
|   is designed to work with RXTX by being dynamically linked with it,
|   is considered a "work that uses the Library" subject to the terms and
|   conditions of the GNU Lesser General Public License.
|
|   The following has been added to the RXTX License to remove
|   any confusion about linking to RXTX.   We want to allow in part what
|   section 5, paragraph 2 of the LGPL does not permit in the special
|   case of linking over a controlled interface.  The intent is to add a
|   Java Specification Request or standards body defined interface in the
|   future as another exception but one is not currently available.
|
|   http://www.fsf.org/licenses/gpl-faq.html#LinkingOverControlledInterface
|
|   As a special exception, the copyright holders of RXTX give you
|   permission to link RXTX with independent modules that communicate with
|   RXTX solely through the Sun Microsytems CommAPI interface version 2,
|   regardless of the license terms of these independent modules, and to copy
|   and distribute the resulting combined work under terms of your choice,
|   provided that every copy of the combined work is accompanied by a complete
|   copy of the source code of RXTX (the version of RXTX used to produce the
|   combined work), being distributed under the terms of the GNU Lesser General
|   Public License plus this exception.  An independent module is a
|   module which is not derived from or based on RXTX.
|
|   Note that people who make modified versions of RXTX are not obligated
|   to grant this special exception for their modified versions; it is
|   their choice whether to do so.  The GNU Lesser General Public License
|   gives permission to release a modified version without this exception; this
|   exception also makes it possible to release a modified version which
|   carries forward this exception.
|
|   You should have received a copy of the GNU Lesser General Public
|   License along with this library; if not, write to the Free
|   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
|   All trademarks belong to their respective owners.
--------------------------------------------------------------------------*/
package gnu.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TooManyListenersException;

/**
 * LPRPort
 */

final class LPRPort extends ParallelPort {

    class MonitorThread extends Thread {
        private boolean monError = false;
        private boolean monBuffer = false;

        MonitorThread() {}

        @Override
        public void run() {
            eventLoop();
            yield();
        }
    }

    /** Inner class for ParallelInputStream */
    class ParallelInputStream extends InputStream {
        @Override
        public int available() throws IOException {
            if (fd == 0) throw new IOException();
            return nativeavailable();
        }

        @Override
        public int read() throws IOException {
            if (fd == 0) throw new IOException();
            return readByte();
        }

        @Override
        public int read(final byte b[]) throws IOException {
            if (fd == 0) throw new IOException();
            return readArray(b, 0, b.length);
        }

        @Override
        public int read(final byte b[], final int off, final int len) throws IOException {
            if (fd == 0) throw new IOException();
            return readArray(b, off, len);
        }
    }

    /** Inner class for ParallelOutputStream */
    class ParallelOutputStream extends OutputStream {
        @Override
        public synchronized void flush() throws IOException {
            if (fd == 0) throw new IOException();
            // drain();
        }

        @Override
        public synchronized void write(final byte b[]) throws IOException {
            if (fd == 0) throw new IOException();
            writeArray(b, 0, b.length);
        }

        @Override
        public synchronized void write(final byte b[], final int off, final int len) throws IOException {
            if (fd == 0) throw new IOException();
            writeArray(b, off, len);
        }

        @Override
        public synchronized void write(final int b) throws IOException {
            if (fd == 0) throw new IOException();
            writeByte(b);
        }
    }

    static {
        System.loadLibrary("rxtxParallel");
        Initialize();
    }
    private final static boolean debug = false;

    /** Initialize the native library */
    private native static void Initialize();

    /** File descriptor */
    private int fd;
    /** Output stream */
    private final ParallelOutputStream out = new ParallelOutputStream();

    /** Input stream */
    private final ParallelInputStream in = new ParallelInputStream();
    /**
     * return current mode LPT_MODE_SPP, LPT_MODE_PS2, LPT_MODE_EPP, or LPT_MODE_ECP
     */
    private int lprmode = LPT_MODE_ANY;

    /** Receive timeout control */
    private int timeout = 0;
    /** Receive threshold control */
    private int threshold = 1;
    /** Parallel Port Event listener */
    private ParallelPortEventListener PPEventListener;
    /** Thread to monitor data */
    private MonitorThread monThread;

    /** Open the named port */
    public LPRPort(final String name) throws PortInUseException {
        if (debug) System.out.println("LPRPort:LPRPort(" + name + ")");
        /*
         * commapi/javadocs/API_users_guide.html specifies that whenever an application tries to open a port in use by another application the
         * PortInUseException will be thrown I know some didnt like it this way but I'm not sure how to avoid it. We will just be writing to a bogus fd if we
         * catch the exeption Trent
         */
        // try {
        fd = open(name);
        this.name = name;
        // } catch ( PortInUseException e ){}
        if (debug) System.out.println("LPRPort:LPRPort(" + name + ") fd = " + fd);
    }

    /** Add an event listener */
    @Override
    public synchronized void addEventListener(final ParallelPortEventListener lsnr) throws TooManyListenersException {
        if (PPEventListener != null) throw new TooManyListenersException();
        PPEventListener = lsnr;
        monThread = new MonitorThread();
        monThread.start();
    }

    public boolean checkMonitorThread() {
        if (monThread != null) return monThread.isInterrupted();
        return true;
    }

    @Override
    public synchronized void close() {
        if (fd < 0) return;
        nativeClose();
        super.close();
        removeEventListener();

        fd = 0;
        Runtime.getRuntime().gc();
    }

    @Override
    public void disableReceiveFraming() {}

    @Override
    public void disableReceiveThreshold() {
        threshold = 1;
    }

    @Override
    public void disableReceiveTimeout() {
        timeout = 0;
    }

    protected native void drain() throws IOException;

    /** Receive framing control */
    @Override
    public void enableReceiveFraming(final int f) throws UnsupportedCommOperationException {
        throw new UnsupportedCommOperationException("Not supported");
    }

    @Override
    public void enableReceiveThreshold(final int t) {
        if (t > 1)
            threshold = t;
        else
            threshold = 1;
    }

    @Override
    public void enableReceiveTimeout(final int t) {
        if (t > 0)
            timeout = t;
        else
            timeout = 0;
    }

    /** Process ParallelPortEvents */
    native void eventLoop();

    /** Finalize the port */
    @Override
    protected void finalize() {
        if (fd > 0) close();
    }

    @Override
    public native int getInputBufferSize();

    @Override
    public InputStream getInputStream() {
        return in;
    }

    @Override
    public int getMode() {
        return lprmode;
    }

    @Override
    public native int getOutputBufferFree();

    @Override
    public native int getOutputBufferSize();

    @Override
    public OutputStream getOutputStream() {
        return out;
    }

    @Override
    public int getReceiveFramingByte() {
        return 0;
    }

    @Override
    public int getReceiveThreshold() {
        return threshold;
    }

    @Override
    public int getReceiveTimeout() {
        return timeout;
    }

    @Override
    public native boolean isPaperOut();;

    @Override
    public native boolean isPrinterBusy();

    @Override
    public native boolean isPrinterError();

    @Override
    public native boolean isPrinterSelected();

    @Override
    public native boolean isPrinterTimedOut();

    @Override
    public boolean isReceiveFramingEnabled() {
        return false;
    }

    @Override
    public boolean isReceiveThresholdEnabled() {
        return threshold > 1;
    }

    @Override
    public boolean isReceiveTimeoutEnabled() {
        return timeout > 0;
    }

    /** LPRPort read methods */
    protected native int nativeavailable() throws IOException;

    /** Close the port */
    private native void nativeClose();

    @Override
    public synchronized void notifyOnBuffer(final boolean enable) {
        System.out.println("notifyOnBuffer is not implemented yet");
        monThread.monBuffer = enable;
    }

    /**
     * Note: these have to be separate boolean flags because the ParallelPortEvent constants are NOT bit-flags, they are just defined as integers from 1 to 10
     * -DPL
     */
    @Override
    public synchronized void notifyOnError(final boolean enable) {
        System.out.println("notifyOnError is not implemented yet");
        monThread.monError = enable;
    }

    private synchronized native int open(String name) throws PortInUseException;

    protected native int readArray(byte b[], int off, int len) throws IOException;

    protected native int readByte() throws IOException;

    /** Remove the parallel port event listener */
    @Override
    public synchronized void removeEventListener() {
        PPEventListener = null;
        if (monThread != null) {
            monThread.interrupt();
            monThread = null;
        }
    }

    @Override
    public void restart() {
        System.out.println("restart() is not implemented");
    }

    public synchronized boolean sendEvent(final int event, final boolean state) {
        /* Let the native side know its time to die */

        if (fd == 0 || PPEventListener == null || monThread == null) return true;

        switch (event) {
        case ParallelPortEvent.PAR_EV_BUFFER:
            if (monThread.monBuffer) break;
            return false;
        case ParallelPortEvent.PAR_EV_ERROR:
            if (monThread.monError) break;
            return false;
        default:
            System.err.println("unknown event:" + event);
            return false;
        }
        final ParallelPortEvent e = new ParallelPortEvent(this, event, !state, state);
        if (PPEventListener != null) PPEventListener.parallelEvent(e);
        if (fd == 0 || PPEventListener == null || monThread == null)
            return true;
        else {
            try {
                Thread.sleep(50);
            } catch (final Exception exc) {}
            return false;
        }
    }

    /**
     * Input/output buffers These are native stubs...
     */

    @Override
    public native void setInputBufferSize(int size);

    public native boolean setLPRMode(int mode) throws UnsupportedCommOperationException;

    @Override
    public int setMode(final int mode) throws UnsupportedCommOperationException {
        try {
            setLPRMode(mode);
        } catch (final UnsupportedCommOperationException e) {
            e.printStackTrace();
            return -1;
        }
        lprmode = mode;
        return 0;
    }

    @Override
    public native void setOutputBufferSize(int size);

    @Override
    public void suspend() {
        System.out.println("suspend() is not implemented");
    }

    protected native void writeArray(byte b[], int off, int len) throws IOException;

    /** Write to the port */
    protected native void writeByte(int b) throws IOException;
}
