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
package net.sourceforge.plantuml.core;

import net.sourceforge.plantuml.utils.StartUtils;

public enum DiagramType {
	UML, DITAA, DOT, PROJECT, JCCKIT, SALT, FLOW, CREOLE, JUNGLE, CUTE, MATH, LATEX, UNKNOWN;

	static public DiagramType getTypeFromArobaseStart(String s) {
		s = s.toLowerCase();
		// if (s.startsWith("@startuml2")) {
		// return UML2;
		// }
		if (StartUtils.startsWithSymbolAnd("startuml", s)) {
			return UML;
		}
		if (StartUtils.startsWithSymbolAnd("startdot", s)) {
			return DOT;
		}
		if (StartUtils.startsWithSymbolAnd("startjcckit", s)) {
			return JCCKIT;
		}
		if (StartUtils.startsWithSymbolAnd("startditaa", s)) {
			return DITAA;
		}
		if (StartUtils.startsWithSymbolAnd("startproject", s)) {
			return PROJECT;
		}
		if (StartUtils.startsWithSymbolAnd("startsalt", s)) {
			return SALT;
		}
		if (StartUtils.startsWithSymbolAnd("startflow", s)) {
			return FLOW;
		}
		if (StartUtils.startsWithSymbolAnd("startcreole", s)) {
			return CREOLE;
		}
		if (StartUtils.startsWithSymbolAnd("starttree", s)) {
			return JUNGLE;
		}
		if (StartUtils.startsWithSymbolAnd("startcute", s)) {
			return CUTE;
		}
		if (StartUtils.startsWithSymbolAnd("startmath", s)) {
			return MATH;
		}
		if (StartUtils.startsWithSymbolAnd("startlatex", s)) {
			return LATEX;
		}
		return UNKNOWN;
	}
}
