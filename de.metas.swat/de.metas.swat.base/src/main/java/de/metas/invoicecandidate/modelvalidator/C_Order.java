package de.metas.invoicecandidate.modelvalidator;

/*
 * #%L
 * de.metas.swat.base
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

import org.adempiere.ad.modelvalidator.annotations.DocValidate;
import org.adempiere.ad.modelvalidator.annotations.Interceptor;
import org.adempiere.util.Services;
import org.compiere.model.ModelValidator;

import de.metas.adempiere.model.I_C_Order;
import de.metas.invoicecandidate.api.IInvoiceCandidateHandlerBL;

@Interceptor(I_C_Order.class)
public class C_Order
{
	@DocValidate(timings = { ModelValidator.TIMING_AFTER_COMPLETE, ModelValidator.TIMING_AFTER_REACTIVATE, ModelValidator.TIMING_AFTER_CLOSE })
	public void invalidateInvoiceCandidates(final I_C_Order order)
	{
		final IInvoiceCandidateHandlerBL invoiceCandidateHandlerBL = Services.get(IInvoiceCandidateHandlerBL.class);
		invoiceCandidateHandlerBL.invalidateCandidatesFor(order);
	}
}
