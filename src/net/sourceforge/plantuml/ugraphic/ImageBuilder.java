/* ========================================================================
 * PlantUML : a free UML diagram generator
 * ========================================================================
 *
 * (C) Copyright 2009-2017, Arnaud Roques
 *
 * Project Info:  http://plantuml.com
 * 
 * This file is part of PlantUML.
 *
 * PlantUML is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PlantUML distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public
 * License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 *
 * Original Author:  Arnaud Roques
 * 
 *
 */
package net.sourceforge.plantuml.ugraphic;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.concurrent.Semaphore;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import net.sourceforge.plantuml.AnimatedGifEncoder;
import net.sourceforge.plantuml.CMapData;
import net.sourceforge.plantuml.ColorParam;
import net.sourceforge.plantuml.Dimension2DDouble;
import net.sourceforge.plantuml.EmptyImageBuilder;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.FileUtils;
import net.sourceforge.plantuml.ISkinParam;
import net.sourceforge.plantuml.LineParam;
import net.sourceforge.plantuml.OptionFlags;
import net.sourceforge.plantuml.StringUtils;
import net.sourceforge.plantuml.Url;
import net.sourceforge.plantuml.anim.AffineTransformation;
import net.sourceforge.plantuml.anim.Animation;
import net.sourceforge.plantuml.api.ImageDataComplex;
import net.sourceforge.plantuml.api.ImageDataSimple;
import net.sourceforge.plantuml.braille.UGraphicBraille;
import net.sourceforge.plantuml.core.ImageData;
import net.sourceforge.plantuml.eps.EpsStrategy;
import net.sourceforge.plantuml.graphic.HtmlColor;
import net.sourceforge.plantuml.graphic.HtmlColorGradient;
import net.sourceforge.plantuml.graphic.HtmlColorSimple;
import net.sourceforge.plantuml.graphic.HtmlColorTransparent;
import net.sourceforge.plantuml.graphic.HtmlColorUtils;
import net.sourceforge.plantuml.graphic.StringBounder;
import net.sourceforge.plantuml.graphic.UDrawable;
import net.sourceforge.plantuml.mjpeg.MJPEGGenerator;
import net.sourceforge.plantuml.skin.rose.Rose;
import net.sourceforge.plantuml.ugraphic.crossing.UGraphicCrossing;
import net.sourceforge.plantuml.ugraphic.eps.UGraphicEps;
import net.sourceforge.plantuml.ugraphic.g2d.UGraphicG2d;
import net.sourceforge.plantuml.ugraphic.hand.UGraphicHandwritten;
import net.sourceforge.plantuml.ugraphic.html5.UGraphicHtml5;
import net.sourceforge.plantuml.ugraphic.svg.UGraphicSvg;
import net.sourceforge.plantuml.ugraphic.tikz.UGraphicTikz;
import net.sourceforge.plantuml.ugraphic.visio.UGraphicVdx;

public class ImageBuilder {

	private final ColorMapper colorMapper;
	private final double dpiFactor;
	private final HtmlColor mybackcolor;
	private final String metadata;
	private final String warningOrError;
	private final double margin1;
	private final double margin2;
	private final Animation animation;
	private final boolean useHandwritten;

	private UDrawable udrawable;

	private final double externalMargin1;
	private final double externalMargin2;
	private UStroke borderStroke;
	private HtmlColor borderColor;
	private double borderCorner;

	public ImageBuilder(ColorMapper colorMapper, double dpiFactor, HtmlColor mybackcolor, String metadata,
			String warningOrError, double margin1, double margin2, Animation animation, boolean useHandwritten) {
		this.colorMapper = colorMapper;
		this.dpiFactor = dpiFactor;
		this.mybackcolor = mybackcolor;
		this.metadata = metadata;
		this.warningOrError = warningOrError;
		this.margin1 = margin1;
		this.margin2 = margin2;
		this.animation = animation;
		this.useHandwritten = useHandwritten;
		this.externalMargin1 = 0;
		this.externalMargin2 = 0;
		this.borderStroke = null;
		this.borderColor = null;
		this.borderCorner = 0;
	}

	public ImageBuilder(ISkinParam skinParam, double dpiFactor, String metadata, String warningOrError, double margin1,
			double margin2, Animation animation) {
		this(skinParam, dpiFactor, metadata, warningOrError, margin1, margin2, animation, skinParam
				.getBackgroundColor());
	}

	public ImageBuilder(ISkinParam skinParam, double dpiFactor, String metadata, String warningOrError, double margin1,
			double margin2, Animation animation, HtmlColor backColor) {
		final Rose rose = new Rose();
		this.borderColor = rose.getHtmlColor(skinParam, ColorParam.diagramBorder);
		this.borderStroke = skinParam.getThickness(LineParam.diagramBorder, null);
		this.borderCorner = skinParam.getRoundCorner("diagramBorder", null);
		if (borderStroke == null && borderColor != null) {
			this.borderStroke = new UStroke();
		}

		this.colorMapper = skinParam.getColorMapper();
		this.dpiFactor = dpiFactor;
		this.mybackcolor = backColor;
		this.metadata = metadata;
		this.warningOrError = warningOrError;
		this.margin1 = margin1;
		this.margin2 = margin2;
		this.animation = animation;
		this.useHandwritten = skinParam.handwritten();
		this.externalMargin1 = 0;
		this.externalMargin2 = 0;
	}

	public void setUDrawable(UDrawable udrawable) {
		this.udrawable = udrawable;
	}

	public ImageData writeImageTOBEMOVED(FileFormatOption fileFormatOption, OutputStream os) throws IOException {
		final FileFormat fileFormat = fileFormatOption.getFileFormat();
		if (fileFormat == FileFormat.MJPEG) {
			return writeImageMjpeg(os, fileFormat.getDefaultStringBounder());
		} else if (fileFormat == FileFormat.ANIMATED_GIF) {
			return writeImageAnimatedGif(os, fileFormat.getDefaultStringBounder());
		}
		return writeImageInternal(fileFormatOption, os, animation);
	}

	private static Semaphore SEMAPHORE_SMALL;
	private static Semaphore SEMAPHORE_BIG;
	private static int MAX_PRICE = 0;

	public static void setMaxPixel(int max) {
		MAX_PRICE = max / 2;
		SEMAPHORE_SMALL = new Semaphore(MAX_PRICE, true);
		SEMAPHORE_BIG = new Semaphore(MAX_PRICE, true);
	}

	private int getPrice(FileFormatOption fileFormatOption, Dimension2D dim) {
		// if (fileFormatOption.getFileFormat() != FileFormat.PNG) {
		// return 0;
		// }
		if (MAX_PRICE == 0) {
			return 0;
		}
		final int price = Math.min(MAX_PRICE, ((int) (dim.getHeight() * dpiFactor))
				* ((int) (dim.getWidth() * dpiFactor)));
		return price;
	}

	private Semaphore getSemaphore(int price) {
		if (price == 0) {
			return null;
		}
		if (price == MAX_PRICE) {
			return SEMAPHORE_BIG;
		}
		return SEMAPHORE_SMALL;
	}

	private ImageData writeImageInternal(FileFormatOption fileFormatOption, OutputStream os, Animation animationArg)
			throws IOException {
		Dimension2D dim = getFinalDimension(fileFormatOption.getDefaultStringBounder());
		double dx = 0;
		double dy = 0;
		if (animationArg != null) {
			final MinMax minmax = animation.getMinMax(dim);
			animationArg.setDimension(dim);
			dim = minmax.getDimension();
			dx = -minmax.getMinX();
			dy = -minmax.getMinY();
		}

		final int price = getPrice(fileFormatOption, dim);
		final Semaphore semaphore = getSemaphore(price);
		if (semaphore != null) {
			try {
				semaphore.acquire(price);
			} catch (InterruptedException e) {
				e.printStackTrace();
				throw new IOException(e);
			}
		}
		try {
			final UGraphic2 ug = createUGraphic(fileFormatOption, dim, animationArg, dx, dy);
			UGraphic ug2 = ug;
			if (externalMargin1 > 0) {
				ug2 = ug2.apply(new UTranslate(externalMargin1, externalMargin1));
			}
			if (borderStroke != null) {
				final HtmlColor color = borderColor == null ? HtmlColorUtils.BLACK : borderColor;
				final URectangle shape = new URectangle(
						dim.getWidth() - externalMargin() - borderStroke.getThickness(), dim.getHeight()
								- externalMargin() - borderStroke.getThickness(), borderCorner, borderCorner);
				ug2.apply(new UChangeColor(color)).apply(borderStroke).draw(shape);
			}
			if (externalMargin1 > 0) {
				ug2 = ug2.apply(new UTranslate(externalMargin2, externalMargin2));
			}
			ug2 = ug2.apply(new UTranslate(margin1, margin1));
			final UGraphic ugDecored = handwritten(ug2);
			udrawable.drawU(ugDecored);
			ugDecored.flushUg();
			ug.writeImageTOBEMOVED(os, metadata, 96);
			os.flush();

			if (ug instanceof UGraphicG2d) {
				final Set<Url> urls = ((UGraphicG2d) ug).getAllUrlsEncountered();
				if (urls.size() > 0) {
					final CMapData cmap = CMapData.cmapString(urls, dpiFactor);
					return new ImageDataComplex(dim, cmap, warningOrError);
				}
			}
			return new ImageDataSimple(dim);
		} finally {
			if (semaphore != null) {
				semaphore.release(price);
			}
		}

	}

	private double externalMargin() {
		return 2 * (externalMargin1 + externalMargin2);
	}

	public Dimension2D getFinalDimension(StringBounder stringBounder) {
		final LimitFinder limitFinder = new LimitFinder(stringBounder, true);
		udrawable.drawU(limitFinder);
		Dimension2D dim = new Dimension2DDouble(limitFinder.getMaxX() + 1 + margin1 + margin2 + externalMargin(),
				limitFinder.getMaxY() + 1 + margin1 + margin2 + externalMargin());
		return dim;
	}

	private UGraphic handwritten(UGraphic ug) {
		if (useHandwritten) {
			return new UGraphicHandwritten(ug);
		}
		if (OptionFlags.OMEGA_CROSSING) {
			return new UGraphicCrossing(ug);
		} else {
			return ug;
		}
	}

	private ImageData writeImageMjpeg(OutputStream os, StringBounder stringBounder) throws IOException {

		final LimitFinder limitFinder = new LimitFinder(stringBounder, true);
		udrawable.drawU(limitFinder);
		final Dimension2D dim = new Dimension2DDouble(limitFinder.getMaxX() + 1 + margin1 + margin2,
				limitFinder.getMaxY() + 1 + margin1 + margin2);

		final File f = new File("c:/tmp.avi");

		final int nbframe = 100;

		final MJPEGGenerator m = new MJPEGGenerator(f, getAviImage(null).getWidth(null), getAviImage(null).getHeight(
				null), 12.0, nbframe);
		for (int i = 0; i < nbframe; i++) {
			// AffineTransform at = AffineTransform.getRotateInstance(1.0);
			AffineTransform at = AffineTransform.getTranslateInstance(dim.getWidth() / 2, dim.getHeight() / 2);
			at.rotate(90.0 * Math.PI / 180.0 * i / 100);
			at.translate(-dim.getWidth() / 2, -dim.getHeight() / 2);
			// final AffineTransform at = AffineTransform.getTranslateInstance(i, 0);
			// final ImageIcon ii = new ImageIcon(getAviImage(at));
			// m.addImage(ii.getImage());
			throw new UnsupportedOperationException();
		}
		m.finishAVI();

		FileUtils.copyToStream(f, os);

		return new ImageDataSimple(dim);

	}

	private ImageData writeImageAnimatedGif(OutputStream os, StringBounder stringBounder) throws IOException {

		final LimitFinder limitFinder = new LimitFinder(stringBounder, true);
		udrawable.drawU(limitFinder);
		final Dimension2D dim = new Dimension2DDouble(limitFinder.getMaxX() + 1 + margin1 + margin2,
				limitFinder.getMaxY() + 1 + margin1 + margin2);

		final MinMax minmax = animation.getMinMax(dim);

		final AnimatedGifEncoder e = new AnimatedGifEncoder();
		// e.setQuality(1);
		e.setRepeat(0);
		e.start(os);
		// e.setDelay(1000); // 1 frame per sec
		// e.setDelay(100); // 10 frame per sec
		e.setDelay(60); // 16 frame per sec
		// e.setDelay(50); // 20 frame per sec

		for (AffineTransformation at : animation.getAll()) {
			final ImageIcon ii = new ImageIcon(getAviImage(at));
			e.addFrame((BufferedImage) ii.getImage());
		}
		e.finish();
		return new ImageDataSimple(dim);

	}

	private Image getAviImage(AffineTransformation affineTransform) throws IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writeImageInternal(new FileFormatOption(FileFormat.PNG), baos, Animation.singleton(affineTransform));
		baos.close();

		final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		final Image im = ImageIO.read(bais);
		bais.close();
		return im;
	}

	private UGraphic2 createUGraphic(FileFormatOption fileFormatOption, final Dimension2D dim, Animation animationArg,
			double dx, double dy) {
		final FileFormat fileFormat = fileFormatOption.getFileFormat();
		switch (fileFormat) {
		case PNG:
			return createUGraphicPNG(colorMapper, dpiFactor, dim, mybackcolor, animationArg, dx, dy);
		case SVG:
			return createUGraphicSVG(colorMapper, dpiFactor, dim, mybackcolor, fileFormatOption.getSvgLinkTarget());
		case EPS:
			return new UGraphicEps(colorMapper, EpsStrategy.getDefault2());
		case EPS_TEXT:
			return new UGraphicEps(colorMapper, EpsStrategy.WITH_MACRO_AND_TEXT);
		case HTML5:
			return new UGraphicHtml5(colorMapper);
		case VDX:
			return new UGraphicVdx(colorMapper);
		case LATEX:
			return new UGraphicTikz(colorMapper, dpiFactor, true);
		case LATEX_NO_PREAMBLE:
			return new UGraphicTikz(colorMapper, dpiFactor, false);
		case BRAILLE_PNG:
			return new UGraphicBraille(colorMapper, fileFormat);
		default:
			throw new UnsupportedOperationException(fileFormat.toString());
		}
	}

	private UGraphic2 createUGraphicSVG(ColorMapper colorMapper, double scale, Dimension2D dim, HtmlColor mybackcolor,
			String svgLinkTarget) {
		Color backColor = Color.WHITE;
		if (mybackcolor instanceof HtmlColorSimple) {
			backColor = colorMapper.getMappedColor(mybackcolor);
		}
		final UGraphicSvg ug;
		if (mybackcolor instanceof HtmlColorGradient) {
			ug = new UGraphicSvg(colorMapper, (HtmlColorGradient) mybackcolor, false, scale, svgLinkTarget);
		} else if (backColor == null || backColor.equals(Color.WHITE)) {
			ug = new UGraphicSvg(colorMapper, false, scale, svgLinkTarget);
		} else {
			ug = new UGraphicSvg(colorMapper, StringUtils.getAsHtml(backColor), false, scale, svgLinkTarget);
		}
		return ug;

	}

	private UGraphic2 createUGraphicPNG(ColorMapper colorMapper, double dpiFactor, final Dimension2D dim,
			HtmlColor mybackcolor, Animation affineTransforms, double dx, double dy) {
		Color backColor = Color.WHITE;
		if (mybackcolor instanceof HtmlColorSimple) {
			backColor = colorMapper.getMappedColor(mybackcolor);
		} else if (mybackcolor instanceof HtmlColorTransparent) {
			backColor = null;
		}

		/*
		 * if (rotation) { builder = new EmptyImageBuilder((int) (dim.getHeight() * dpiFactor), (int) (dim.getWidth() *
		 * dpiFactor), backColor); graphics2D = builder.getGraphics2D(); graphics2D.rotate(-Math.PI / 2);
		 * graphics2D.translate(-builder.getBufferedImage().getHeight(), 0); } else {
		 */
		final EmptyImageBuilder builder = new EmptyImageBuilder((int) (dim.getWidth() * dpiFactor),
				(int) (dim.getHeight() * dpiFactor), backColor);
		final Graphics2D graphics2D = builder.getGraphics2D();

		// }
		final UGraphicG2d ug = new UGraphicG2d(colorMapper, graphics2D, dpiFactor, affineTransforms == null ? null
				: affineTransforms.getFirst(), dx, dy);
		ug.setBufferedImage(builder.getBufferedImage());
		final BufferedImage im = ((UGraphicG2d) ug).getBufferedImage();
		if (mybackcolor instanceof HtmlColorGradient) {
			ug.apply(new UChangeBackColor(mybackcolor)).draw(new URectangle(im.getWidth(), im.getHeight()));
		}

		return ug;
	}

}
