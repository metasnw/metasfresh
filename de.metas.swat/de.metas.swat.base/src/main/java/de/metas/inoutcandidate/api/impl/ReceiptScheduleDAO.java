package de.metas.inoutcandidate.api.impl;

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


import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.adempiere.ad.dao.IQueryBL;
import org.adempiere.ad.dao.IQueryBuilder;
import org.adempiere.ad.dao.IQueryOrderBy;
import org.adempiere.ad.dao.impl.EqualsQueryFilter;
import org.adempiere.ad.table.api.IADTableDAO;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.compiere.model.IQuery;
import org.compiere.model.I_M_InOut;
import org.compiere.process.DocAction;

import de.metas.inout.model.I_M_InOutLine;
import de.metas.inoutcandidate.api.IReceiptScheduleDAO;
import de.metas.inoutcandidate.model.I_M_ReceiptSchedule;
import de.metas.inoutcandidate.model.I_M_ReceiptSchedule_Alloc;

public class ReceiptScheduleDAO implements IReceiptScheduleDAO
{

	@Override
	public Iterator<I_M_ReceiptSchedule> retrieve(final IQuery<I_M_ReceiptSchedule> query)
	{
		// Note that we can't filter by IsError in the where clause, because it wouldn't work with pagination.
		// Background is that the number of candidates with "IsError=Y" might increase during the run.
		final IQueryOrderBy orderBy = Services.get(IQueryBL.class).createQueryOrderByBuilder(I_M_ReceiptSchedule.class)
				.addColumn(I_M_ReceiptSchedule.COLUMNNAME_HeaderAggregationKey)
				.createQueryOrderBy();

		// NOTE: we are not touching the old Query but clone it first and customize the new copy.
		final IQuery<I_M_ReceiptSchedule> queryNew = query.copy();

		return queryNew
				.setOrderBy(orderBy)
				.setOption(IQuery.OPTION_GuaranteedIteratorRequired, true) // need guaranteed iterator, because the number of unprocessed items is reduced by that process using this query
				.iterate(I_M_ReceiptSchedule.class);
	}

	@Override
	public I_M_ReceiptSchedule retrieveForRecord(Object model)
	{
		final Properties ctx = InterfaceWrapperHelper.getCtx(model);
		final String tableName = InterfaceWrapperHelper.getModelTableName(model);
		final int recordId = InterfaceWrapperHelper.getId(model);
		final String trxName = InterfaceWrapperHelper.getTrxName(model);
		return retrieveForRecord(ctx, tableName, recordId, trxName);
	}

	private I_M_ReceiptSchedule retrieveForRecord(final Properties ctx, final String tableName, final int recordId, final String trxName)
	{
		final int adTableId = Services.get(IADTableDAO.class).retrieveTableId(tableName);

		return Services.get(IQueryBL.class).createQueryBuilder(I_M_ReceiptSchedule.class, ctx, trxName)
				.filter(new EqualsQueryFilter<I_M_ReceiptSchedule>(I_M_ReceiptSchedule.COLUMNNAME_AD_Table_ID, adTableId))
				.filter(new EqualsQueryFilter<I_M_ReceiptSchedule>(I_M_ReceiptSchedule.COLUMNNAME_Record_ID, recordId))
				.filterByClientId()
				.create()
				.firstOnly(I_M_ReceiptSchedule.class);

	}

	@Override
	public List<I_M_ReceiptSchedule_Alloc> retrieveRsaForRs(final I_M_ReceiptSchedule rs)
	{
		final String trxName = InterfaceWrapperHelper.getTrxName(rs);
		return retrieveRsaForRs(rs, trxName);
	}

	@Override
	public List<I_M_ReceiptSchedule_Alloc> retrieveRsaForRs(final I_M_ReceiptSchedule rs, final String trxName)
	{
		final IQueryBuilder<I_M_ReceiptSchedule_Alloc> queryBuilder = createRsaForRsQueryBuilder(rs, I_M_ReceiptSchedule_Alloc.class, trxName);
		return queryBuilder.create().list(I_M_ReceiptSchedule_Alloc.class);
	}

	@Override
	public <T extends I_M_ReceiptSchedule_Alloc> IQueryBuilder<T> createRsaForRsQueryBuilder(final I_M_ReceiptSchedule rs, final Class<T> receiptScheduleAllocClass, final String trxName)
	{
		Check.assumeNotNull(rs, "rs not null");

		final Properties ctx = InterfaceWrapperHelper.getCtx(rs);

		final IQueryBuilder<T> queryBuilder = Services.get(IQueryBL.class).createQueryBuilder(receiptScheduleAllocClass, ctx, trxName)
				.addEqualsFilter(I_M_ReceiptSchedule_Alloc.COLUMNNAME_M_ReceiptSchedule_ID, rs.getM_ReceiptSchedule_ID());

		queryBuilder.orderBy()
				.addColumn(I_M_ReceiptSchedule_Alloc.COLUMNNAME_M_ReceiptSchedule_Alloc_ID);

		return queryBuilder;
	}

	@Override
	public <T extends I_M_ReceiptSchedule_Alloc> IQueryBuilder<T> createRsaForRsQueryBuilder(final I_M_ReceiptSchedule rs, final Class<T> receiptScheduleAllocClass)
	{
		final String trxName = InterfaceWrapperHelper.getTrxName(rs);
		return createRsaForRsQueryBuilder(rs, receiptScheduleAllocClass, trxName);
	}

	@Override
	public I_M_ReceiptSchedule_Alloc retrieveRsaForRs(final I_M_ReceiptSchedule receiptSchedule, final I_M_InOutLine receiptLine)
	{
		final IQueryBuilder<I_M_ReceiptSchedule_Alloc> queryBuilder = Services.get(IQueryBL.class).createQueryBuilder(I_M_ReceiptSchedule_Alloc.class, receiptLine)
				.filter(new EqualsQueryFilter<I_M_ReceiptSchedule_Alloc>(I_M_ReceiptSchedule_Alloc.COLUMNNAME_M_ReceiptSchedule_ID, receiptSchedule.getM_ReceiptSchedule_ID()))
				.filter(new EqualsQueryFilter<I_M_ReceiptSchedule_Alloc>(I_M_ReceiptSchedule_Alloc.COLUMNNAME_M_InOutLine_ID, receiptLine.getM_InOutLine_ID()));

		queryBuilder.orderBy()
				.addColumn(I_M_ReceiptSchedule_Alloc.COLUMNNAME_M_ReceiptSchedule_Alloc_ID);

		final I_M_ReceiptSchedule_Alloc existingRsa = queryBuilder.create()
				.firstOnly(I_M_ReceiptSchedule_Alloc.class);

		return existingRsa;
	}

	@Override
	public List<I_M_ReceiptSchedule_Alloc> retrieveRsaForInOutLine(final org.compiere.model.I_M_InOutLine line)
	{
		final IQueryBuilder<I_M_ReceiptSchedule_Alloc> queryBuilder = Services.get(IQueryBL.class)
				.createQueryBuilder(I_M_ReceiptSchedule_Alloc.class, line)
				.filter(new EqualsQueryFilter<I_M_ReceiptSchedule_Alloc>(I_M_ReceiptSchedule_Alloc.COLUMNNAME_M_InOutLine_ID, line.getM_InOutLine_ID()));

		queryBuilder.orderBy()
				.addColumn(I_M_ReceiptSchedule_Alloc.COLUMNNAME_M_ReceiptSchedule_Alloc_ID);

		return queryBuilder
				.create()
				.list(I_M_ReceiptSchedule_Alloc.class);
	}

	@Override
	public List<I_M_InOut> retrieveCompletedReceipts(final I_M_ReceiptSchedule receiptSchedule)
	{
		return createRsaForRsQueryBuilder(receiptSchedule, I_M_ReceiptSchedule_Alloc.class)
				.andCollect(I_M_ReceiptSchedule_Alloc.COLUMN_M_InOutLine_ID)
				.andCollect(I_M_InOutLine.COLUMN_M_InOut_ID)
				.addInArrayFilter(I_M_InOut.COLUMNNAME_DocStatus, DocAction.STATUS_Completed)
				.create()
				.list();
	}

}
