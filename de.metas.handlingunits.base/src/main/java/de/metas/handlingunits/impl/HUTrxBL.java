package de.metas.handlingunits.impl;

/*
 * #%L
 * de.metas.handlingunits.base
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


import java.util.Date;
import java.util.List;

import org.adempiere.ad.dao.cache.impl.TableRecordCacheLocal;
import org.adempiere.inout.service.IMTransactionBL;
import org.adempiere.model.IContextAware;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.compiere.model.I_C_UOM;
import org.compiere.model.I_M_Transaction;

import de.metas.handlingunits.IHUContext;
import de.metas.handlingunits.IHUContextFactory;
import de.metas.handlingunits.IHUTransactionProcessor;
import de.metas.handlingunits.IHUTrxBL;
import de.metas.handlingunits.IHUTrxListener;
import de.metas.handlingunits.IHandlingUnitsBL;
import de.metas.handlingunits.IHandlingUnitsDAO;
import de.metas.handlingunits.IMutableHUContext;
import de.metas.handlingunits.allocation.IAllocationDestination;
import de.metas.handlingunits.allocation.IAllocationRequest;
import de.metas.handlingunits.allocation.IAllocationResult;
import de.metas.handlingunits.allocation.IAllocationSource;
import de.metas.handlingunits.allocation.IHUContextProcessorExecutor;
import de.metas.handlingunits.allocation.impl.AllocationUtils;
import de.metas.handlingunits.allocation.impl.HUContextProcessorExecutor;
import de.metas.handlingunits.allocation.impl.HULoader;
import de.metas.handlingunits.allocation.impl.HUProducerDestination;
import de.metas.handlingunits.allocation.impl.MTransactionAllocationSourceDestination;
import de.metas.handlingunits.attribute.storage.IAttributeStorage;
import de.metas.handlingunits.attribute.storage.IAttributeStorageFactory;
import de.metas.handlingunits.attribute.storage.impl.NullAttributeStorage;
import de.metas.handlingunits.model.I_M_HU;
import de.metas.handlingunits.model.I_M_HU_Item;
import de.metas.handlingunits.model.I_M_HU_PI;
import de.metas.handlingunits.model.I_M_HU_Trx_Line;
import de.metas.handlingunits.storage.IHUStorage;
import de.metas.handlingunits.storage.IHUStorageFactory;

public class HUTrxBL implements IHUTrxBL
{
	private final CompositeHUTrxListener _trxListeners = new CompositeHUTrxListener();

	private static final String DYNATTR_TableRecord = TrxLineTableRecordCacheLocal.class.getName();

	private static final class TrxLineTableRecordCacheLocal extends TableRecordCacheLocal<I_M_HU_Trx_Line>
	{
		public TrxLineTableRecordCacheLocal(final I_M_HU_Trx_Line trxLine)
		{
			super(trxLine);
		}

		@Override
		protected int getAD_Table_ID()
		{
			final I_M_HU_Trx_Line trxLine = getParentModel();
			return trxLine.getAD_Table_ID();
		}

		@Override
		protected int getRecord_ID()
		{
			final I_M_HU_Trx_Line trxLine = getParentModel();
			return trxLine.getRecord_ID();
		}

		@Override
		protected void setTableAndRecordId(final int adTableId, final int recordId)
		{
			final I_M_HU_Trx_Line trxLine = getParentModel();
			trxLine.setAD_Table_ID(adTableId);
			trxLine.setRecord_ID(recordId);
		}
	}

	@Override
	public void addListener(final IHUTrxListener trxListener)
	{
		_trxListeners.addListener(trxListener);
	}

	@Override
	public IHUTrxListener getHUTrxListeners()
	{
		return _trxListeners;
	}

	@Override
	public List<IHUTrxListener> getHUTrxListenersList()
	{
		return _trxListeners.asList();
	}

	@Override
	public List<I_M_HU> transferIncomingToHUs(final I_M_Transaction mtrx, final I_M_HU_PI huPI)
	{
		Check.assume(Services.get(IMTransactionBL.class).isInboundTransaction(mtrx),
				"mtrx shall be inbound transaction: {}", mtrx);

		final IContextAware contextProvider = InterfaceWrapperHelper.getContextAware(mtrx);
		final IMutableHUContext huContext = Services.get(IHandlingUnitsBL.class).createMutableHUContext(contextProvider);

		final IAllocationSource source = new MTransactionAllocationSourceDestination(mtrx);
		final HUProducerDestination destination = new HUProducerDestination(huPI);
		final HULoader loader = new HULoader(source, destination);

		final I_C_UOM uom = Services.get(IHandlingUnitsBL.class).getC_UOM(mtrx);
		final IAllocationRequest request = AllocationUtils.createQtyRequest(
				huContext,
				mtrx.getM_Product(),
				mtrx.getMovementQty(),
				uom, mtrx.getMovementDate(),
				mtrx);

		loader.load(request);

		return destination.getCreatedHUs();
	}

	@Override
	public void transfer(
			final IHUContext huContext,
			final IAllocationSource source,
			final IAllocationDestination destination,
			final IAllocationRequest request)
	{
		Check.assumeNotNull(source, "source not null");
		Check.assumeNotNull(destination, "destination not null");
		Check.assumeNotNull(request, "request not null");

		final HULoader loader = new HULoader(source, destination);
		loader.load(request);
	}

	@Override
	public List<I_M_HU> extractIncludedHUs(final List<I_M_HU> sourceHUs, final int huQty, final I_M_HU_PI destinationHuPI)
	{
		// TODO: implement extractIncludedHUs
		throw new UnsupportedOperationException("Method not implemented");
	}

	private IHUTransactionProcessor createHUTransactionProcessor(final IHUContext huContext)
	{
		final HUTransactionProcessor trxProcessor = new HUTransactionProcessor(huContext);
		return trxProcessor;
	}

	@Override
	public void createTrx(final IHUContext huContext, final IAllocationResult result)
	{
		final IHUTransactionProcessor trxProcessor = createHUTransactionProcessor(huContext);
		trxProcessor.createTrx(result);
	}

	@Override
	public Date getDateTrx(final I_M_HU_Trx_Line line)
	{
		return line.getDateTrx();
	}

	private final TrxLineTableRecordCacheLocal getTrxLineTableRecordCacheLocal(final I_M_HU_Trx_Line trxLine)
	{
		Check.assumeNotNull(trxLine, "trxLine not null");
		TrxLineTableRecordCacheLocal recordRef = InterfaceWrapperHelper.getDynAttribute(trxLine, HUTrxBL.DYNATTR_TableRecord);
		if (recordRef == null)
		{
			recordRef = new TrxLineTableRecordCacheLocal(trxLine);
			InterfaceWrapperHelper.setDynAttribute(trxLine, HUTrxBL.DYNATTR_TableRecord, recordRef);
		}
		return recordRef;
	}

	@Override
	public <T> T getReferencedObjectOrNull(final I_M_HU_Trx_Line trxLine, final Class<T> modelClass)
	{
		final TrxLineTableRecordCacheLocal recordRef = getTrxLineTableRecordCacheLocal(trxLine);
		return recordRef.getValue(modelClass);
	}

	@Override
	public void setReferencedObject(final I_M_HU_Trx_Line trxLine, final Object referencedModel)
	{
		final TrxLineTableRecordCacheLocal recordRef = getTrxLineTableRecordCacheLocal(trxLine);
		recordRef.setValue(referencedModel);
	}

	@Override
	public void reverseTrxLines(final IHUContext huContext, final List<I_M_HU_Trx_Line> trxLines)
	{
		final IHUTransactionProcessor trxProcessor = createHUTransactionProcessor(huContext);
		trxProcessor.reverseTrxLines(trxLines);
	}

	@Override
	public void setParentHU(final IHUContext huContext, final I_M_HU_Item parentHUItem, final I_M_HU hu)
	{
		final boolean destroyOldParentIfEmptyStorage = true;
		setParentHU(huContext, parentHUItem, hu, destroyOldParentIfEmptyStorage);
	}

	@Override
	public void setParentHU(final IHUContext huContext, final I_M_HU_Item parentHUItem, final I_M_HU hu, final boolean destroyOldParentIfEmptyStorage)
	{
		// TODO: handle in HUTrx / allocation

		Check.assumeNotNull(hu, "HU not null");

		//
		// Important: force pre-set HU in current transaction; all future assignments and data retrieval shall be done in current Trx
		// Afterwards, set the HU trx back to it's original one
		//
		final String huTrxNameOld = InterfaceWrapperHelper.getTrxName(hu);
		try
		{
			final String huTrxNameNew = huContext.getTrxName();
			InterfaceWrapperHelper.setTrxName(hu, huTrxNameNew);

			setParentHU0(huContext, parentHUItem, hu, destroyOldParentIfEmptyStorage);
		}
		finally
		{
			InterfaceWrapperHelper.setTrxName(hu, huTrxNameOld);
		}
	}

	/**
	 * Actual processing for HU (set parent & rollup incremental)
	 *
	 * @param huContext
	 * @param parentHUItem
	 * @param hu
	 * @param destroyOldParentIfEmptyStorage
	 */
	private final void setParentHU0(final IHUContext huContext, final I_M_HU_Item parentHUItem, final I_M_HU hu, final boolean destroyOldParentIfEmptyStorage)
	{
		final IHandlingUnitsDAO handlingUnitsDAO = Services.get(IHandlingUnitsDAO.class);
		final IHandlingUnitsBL handlingUnitsBL = Services.get(IHandlingUnitsBL.class);

		//
		// Make sure hu's Parent will change
		if (parentHUItem == null)
		{
			if (handlingUnitsDAO.retrieveParentItem(hu) == null)
			{
				//
				// Nothing was changed: parent was null before
				return;
			}
		}
		else if (hu.getM_HU_Item_Parent_ID() == parentHUItem.getM_HU_Item_ID())
		{
			//
			// Nothing was changed: same references
			return;
		}

		final IAttributeStorageFactory attributeStorageFactory = huContext.getHUAttributeStorageFactory();
		final IAttributeStorage huAttributes = attributeStorageFactory.getAttributeStorage(hu);

		//
		// Fire attribute storage removed on old parent
		final IAttributeStorage parentAttributesOld;
		final I_M_HU_Item parentHUItemOld = handlingUnitsDAO.retrieveParentItem(hu);

		final I_M_HU parentHUOld;
		if (parentHUItemOld != null)
		{
			parentHUOld = parentHUItemOld.getM_HU();
			parentAttributesOld = attributeStorageFactory.getAttributeStorage(parentHUOld);
		}
		else
		{
			parentHUOld = null;
			parentAttributesOld = NullAttributeStorage.instance;
		}

		final boolean huPureVirtual = handlingUnitsBL.isPureVirtual(hu);
		if (!NullAttributeStorage.instance.equals(parentAttributesOld)
				&& !huPureVirtual) // don't propagate pure-virtual HUs; they normally get automatically re-propagated (i.e WeightNet)
		{
			parentAttributesOld.onChildAttributeStorageRemoved(huAttributes);
		}

		//
		// Revert rollup of the target HU's (the one we are assigning to the new parent) old parent storage
		final IHUStorageFactory huStorageFactory = huContext.getHUStorageFactory();
		final IHUStorage huStorageOld = huStorageFactory.getStorage(hu);
		huStorageOld.rollupRevert();

		//
		// Actually unlink the HU from old parent and link it to new parent
		// NOTE: we need to do this AFTER we notify the old parent storages
		handlingUnitsDAO.setParentItem(hu, parentHUItem);

		//
		// Rollup the target HU's (the one we are assigning to the new parent) new parent storage
		final IHUStorage huStorageNew = huStorageFactory.getStorage(hu);
		huStorageNew.rollup();

		//
		// Fire attribute storage added on new parent
		final IAttributeStorage parentAttributesNew;
		if (parentHUItem != null)
		{
			final I_M_HU parentHUNew = parentHUItem.getM_HU();
			parentAttributesNew = attributeStorageFactory.getAttributeStorage(parentHUNew);
		}
		else
		{
			parentAttributesNew = NullAttributeStorage.instance;
		}

		if (!NullAttributeStorage.instance.equals(parentAttributesNew))
		{
			parentAttributesNew.onChildAttributeStorageAdded(huAttributes);
		}

		huContext.getTrxListeners().huParentChanged(hu, parentHUItemOld);

		//
		// If allowed,
		// Mark old HU destroyed if that's the case
		if (destroyOldParentIfEmptyStorage
				&& parentHUOld != null && parentHUOld.getM_HU_ID() > 0)
		{
			handlingUnitsBL.destroyIfEmptyStorage(huContext, parentHUOld);
		}
	}

	@Override
	public IHUContextProcessorExecutor createHUContextProcessorExecutor(final IHUContext huContext)
	{
		final HUContextProcessorExecutor executor = new HUContextProcessorExecutor(huContext);
		return executor;
	}

	@Override
	public IHUContextProcessorExecutor createHUContextProcessorExecutor(final IContextAware context)
	{
		final IHUContext huContext = Services.get(IHUContextFactory.class).createMutableHUContextForProcessing(context);
		final HUContextProcessorExecutor executor = new HUContextProcessorExecutor(huContext);
		return executor;
	}

}
