package de.metas.procurement.base.event.process;

import org.compiere.process.SvrProcess;

import de.metas.procurement.base.event.impl.PMMWeekReportEventsProcessor;

/*
 * #%L
 * de.metas.procurement.base
 * %%
 * Copyright (C) 2016 metas GmbH
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

public class PMM_WeekReport_Event_ProcessAllNow extends SvrProcess
{

	@Override
	protected String doIt() throws Exception
	{
		PMMWeekReportEventsProcessor.newInstance().processAll();
		return MSG_OK;
	}
}
