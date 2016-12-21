package gde.mdl.ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;

import javax.swing.JComponent;
import javax.swing.JLayer;
import javax.swing.Timer;
import javax.swing.plaf.LayerUI;

public class WaitLayerUI<T extends Component> extends LayerUI<T> implements ActionListener {
	private static final long serialVersionUID = 1L;
	private boolean mIsRunning;
	private boolean mIsFadingOut;
	private Timer mTimer;

	private int mAngle;
	private int mFadeCount;
	private final int mFadeLimit = 15;

	@Override
	public void actionPerformed(final ActionEvent e) {
		if (mIsRunning) {
			firePropertyChange("tick", 0, 1);
			mAngle += 3;
			if (mAngle >= 360) {
				mAngle = 0;
			}
			if (mIsFadingOut) {
				if (--mFadeCount <= 0) {
					mIsRunning = false;
					mTimer.stop();
				}
			} else if (mFadeCount < mFadeLimit) {
				mFadeCount++;
			}
		}
	}

	@Override
	public void applyPropertyChange(final PropertyChangeEvent pce, @SuppressWarnings("rawtypes") final JLayer l) {
		if ("tick".equals(pce.getPropertyName())) {
			l.repaint();
		}
	}

	@Override
	public void paint(final Graphics g, final JComponent c) {
		final int w = c.getWidth();
		final int h = c.getHeight();

		// Paint the view.
		super.paint(g, c);

		if (!mIsRunning) {
			return;
		}

		final Graphics2D g2 = (Graphics2D) g.create();

		final float fade = (float) mFadeCount / (float) mFadeLimit;
		// Gray it out.
		final Composite urComposite = g2.getComposite();
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .5f * fade));
		g2.fillRect(0, 0, w, h);
		g2.setComposite(urComposite);

		// Paint the wait indicator.
		final int s = Math.min(w, h) / 5;
		final int cx = w / 2;
		final int cy = h / 2;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setStroke(new BasicStroke(s / 4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g2.setPaint(Color.white);
		g2.rotate(Math.PI * mAngle / 180, cx, cy);
		for (int i = 0; i < 12; i++) {
			final float scale = (11.0f - i) / 11.0f;
			g2.drawLine(cx + s, cy, cx + s * 2, cy);
			g2.rotate(-Math.PI / 6, cx, cy);
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, scale * fade));
		}

		g2.dispose();
	}

	public void start() {
		if (mIsRunning) {
			return;
		}

		// Run a thread for animation.
		mIsRunning = true;
		mIsFadingOut = false;
		mFadeCount = 0;
		final int fps = 24;
		final int tick = 1000 / fps;
		mTimer = new Timer(tick, this);
		mTimer.start();
	}

	public void stop() {
		mIsFadingOut = true;
	}
}