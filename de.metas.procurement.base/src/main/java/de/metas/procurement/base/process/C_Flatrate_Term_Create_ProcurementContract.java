package de.metas.procurement.base.process;

import java.sql.Timestamp;
import java.util.List;

import org.adempiere.ad.process.ISvrProcessDefaultParametersProvider;
import org.adempiere.ad.trx.api.ITrx;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.service.ISysConfigBL;
import org.adempiere.util.Services;
import org.adempiere.util.lang.impl.TableRecordReference;
import org.compiere.model.I_AD_User;
import org.compiere.model.I_C_Calendar;
import org.compiere.model.I_C_Currency;
import org.compiere.model.I_C_Period;
import org.compiere.model.I_C_UOM;
import org.compiere.model.I_M_Product;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;

import de.metas.adempiere.service.ICalendarDAO;
import de.metas.flatrate.api.IFlatrateBL;
import de.metas.flatrate.interfaces.I_C_BPartner;
import de.metas.flatrate.model.I_C_Flatrate_Conditions;
import de.metas.flatrate.model.X_C_Flatrate_Term;
import de.metas.process.Param;
import de.metas.process.Process;
import de.metas.procurement.base.model.I_C_Flatrate_DataEntry;
import de.metas.procurement.base.model.I_C_Flatrate_Term;
import de.metas.procurement.base.model.I_PMM_Product;

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
/**
 * Process used to create procurement contracts
 * 
 * @author metas-dev <dev@metas-fresh.com>
 *
 */
@Process(requiresCurrentRecordWhenCalledFromGear = false)
public class C_Flatrate_Term_Create_ProcurementContract
		extends SvrProcess
		implements ISvrProcessDefaultParametersProvider
{
	private static final String PARAM_NAME_AD_USER_IN_CHARGE_ID = "AD_User_InCharge_ID";

	private static final String SYSCONFIG_AD_USER_IN_CHARGE = "de.metas.procurement.C_Flatrate_Term_Create_ProcurementContract.AD_User_InCharge_ID";

	@Param(mandatory = true, parameterName = "C_Flatrate_Conditions_ID")
	private I_C_Flatrate_Conditions p_C_Flatrate_Conditions;

	@Param(mandatory = true, parameterName = "C_BPartner_ID")
	private I_C_BPartner p_C_BPartner;

	@Param(mandatory = true, parameterName = "Dates")
	private Timestamp p_StartDate;

	@Param(mandatory = true, parameterName = "Dates", parameterTo = true)
	private Timestamp p_EndDate;

	@Param(mandatory = true, parameterName = "PMM_Product_ID")
	private I_PMM_Product p_PMM_Product;

	@Param(mandatory = true, parameterName = "C_UOM_ID")
	private I_C_UOM p_C_UOM;

	@Param(mandatory = false, parameterName = PARAM_NAME_AD_USER_IN_CHARGE_ID)
	private I_AD_User p_AD_User_Incharge;

	@Param(mandatory = true, parameterName = "C_Currency_ID")
	private I_C_Currency p_C_Currency;

	private final IFlatrateBL flatrateBL = Services.get(IFlatrateBL.class);
	private final ICalendarDAO calendarDAO = Services.get(ICalendarDAO.class);

	@Override
	protected String doIt() throws Exception
	{
		// create the term
		final boolean completeIt = false;
		
		final I_M_Product product = p_PMM_Product.getM_Product();

		final I_C_Flatrate_Term term = InterfaceWrapperHelper.create(flatrateBL.createTerm(p_C_BPartner, p_C_Flatrate_Conditions, p_StartDate, p_AD_User_Incharge, product, completeIt), I_C_Flatrate_Term.class);
		if (term == null)
		{
			return "@Success@"; // the process messages will display what went wrong
		}

		term.setC_Currency(p_C_Currency);
		
		// Product
		term.setPMM_Product(p_PMM_Product);
		term.setM_Product(product);
		term.setC_UOM(p_C_UOM);
		term.setContractStatus(X_C_Flatrate_Term.CONTRACTSTATUS_Laufend);

		InterfaceWrapperHelper.save(term);

		// create the dataEntries
		final I_C_Calendar calendar = term.getC_Flatrate_Conditions().getC_Flatrate_Transition().getC_Calendar_Contract();

		final List<I_C_Period> periods = calendarDAO.retrievePeriods(getCtx(), calendar, p_StartDate, p_EndDate, ITrx.TRXNAME_None);
		for (final I_C_Period period : periods)
		{
			final I_C_Flatrate_DataEntry newDataEntry = InterfaceWrapperHelper.newInstance(I_C_Flatrate_DataEntry.class, this);
			newDataEntry.setC_Period(period);
			newDataEntry.setM_Product_DataEntry(term.getM_Product());
			newDataEntry.setC_Currency(term.getC_Currency());
			newDataEntry.setC_Flatrate_Term(term);
			newDataEntry.setC_UOM(term.getC_UOM());
			newDataEntry.setType(I_C_Flatrate_DataEntry.TYPE_Procurement_PeriodBased);
			InterfaceWrapperHelper.save(newDataEntry);
		}

		setRecordToSelectAfterExecution(TableRecordReference.of(term));

		return "@Success@";
	}

	/**
	 * If the given <code>parameterName</code> is {@value #PARAM_NAME_AD_USER_IN_CHARGE_ID},<br>
	 * then the method returns the user set in <code>AD_SysConfig</code> {@value #SYSCONFIG_AD_USER_IN_CHARGE}.
	 */
	@Override
	public Object getParameterDefaultValue(final String parameterName)
	{
		if (!PARAM_NAME_AD_USER_IN_CHARGE_ID.equals(parameterName))
		{
			return DEFAULT_VALUE_NOTAVAILABLE;
		}

		final int ad_Client_ID = Env.getAD_Client_ID(getCtx());
		final int ad_Org_ID = Env.getAD_Org_ID(getCtx());
		final int adUserInChargeId = Services.get(ISysConfigBL.class).getIntValue(SYSCONFIG_AD_USER_IN_CHARGE, -1, ad_Client_ID, ad_Org_ID);
		if (adUserInChargeId < 0)
		{
			return DEFAULT_VALUE_NOTAVAILABLE;
		}

		return adUserInChargeId; // we need to return the ID, not the actual record. Otherwise then lookup logic will be confused.
	}
}
