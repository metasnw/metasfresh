package de.metas.adempiere.report.jasper.server;

/*
 * #%L
 * de.metas.report.jasper.server.base
 * %%
 * Copyright (C) 2015 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */


import java.util.Map;

import net.sf.jasperreports.engine.JRPrintElement;
import net.sf.jasperreports.engine.JasperReportsContext;
import net.sf.jasperreports.engine.export.Cut;
import net.sf.jasperreports.engine.export.CutsInfo;
import net.sf.jasperreports.engine.export.ExporterFilter;
import net.sf.jasperreports.engine.export.JRXlsExporterNature;

/**
 * Extension of {@link JRXlsExporterNature} which implements our custom features (e.g. {@link MetasJRXlsExporter#PROPERTY_COLUMN_HIDDEN}).
 * 
 * @author tsa
 *
 */
public class MetasJRXlsExporterNature extends JRXlsExporterNature
{
	public MetasJRXlsExporterNature(
			final JasperReportsContext jasperReportsContext,
			final ExporterFilter filter,
			final boolean isIgnoreGraphics,
			final boolean isIgnorePageMargins)
	{
		super(jasperReportsContext, filter, isIgnoreGraphics, isIgnorePageMargins);
	}

	@Override
	public void setXProperties(final Map<String, Object> xCutsProperties, final JRPrintElement element)
	{
		super.setXProperties(xCutsProperties, element);
	}

	@Override
	public void setXProperties(CutsInfo xCuts, JRPrintElement element, int row1, int col1, int row2, int col2)
	{
		super.setXProperties(xCuts, element, row1, col1, row2, col2);

		//
		// Mark the xCut (i.e. the column) as hidden if any element from it requied a hidden column.
		if (isColumnHidden(element))
		{
			final Cut cut = xCuts.getCut(col1);
			final Map<String, Object> cutProperties = cut.getPropertiesMap();
			cutProperties.put(MetasJRXlsExporter.PROPERTY_COLUMN_HIDDEN, true);
		}
	}

	/**
	 * @param element
	 * @return true if the element was annotated to require a hidden column
	 */
	private boolean isColumnHidden(final JRPrintElement element)
	{
		if (element.hasProperties()
				&& element.getPropertiesMap().containsProperty(MetasJRXlsExporter.PROPERTY_COLUMN_HIDDEN))
		{
			final boolean defaultValue = false; // not hidden, by default
			// we make this test to avoid reaching the global default value of the property directly
			// and thus skipping the report level one, if present
			return getPropertiesUtil().getBooleanProperty(element, MetasJRXlsExporter.PROPERTY_COLUMN_HIDDEN, defaultValue);
		}
		return false;
	}

}
