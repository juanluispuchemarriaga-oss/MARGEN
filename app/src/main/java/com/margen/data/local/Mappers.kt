package com.margen.data.local

import com.margen.domain.model.CostCategory
import com.margen.domain.model.CostItem
import com.margen.domain.model.HistoryEntry
import com.margen.domain.model.HistoryType
import com.margen.domain.model.Product

fun ProductEntity.toModel(): Product = Product(
    id = id,
    name = name,
    description = description,
    salePricePesos = salePricePesos,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Product.toEntity(): ProductEntity = ProductEntity(
    id = id,
    name = name,
    description = description,
    salePricePesos = salePricePesos,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CostItemEntity.toModel(): CostItem = CostItem(
    id = id,
    productId = productId,
    category = runCatching { CostCategory.valueOf(category) }.getOrDefault(CostCategory.OTHER),
    name = name,
    quantityMilli = quantityMilli,
    unit = unit,
    unitCostPesos = unitCostPesos
)

fun CostItem.toEntity(): CostItemEntity = CostItemEntity(
    id = id,
    productId = productId,
    category = category.name,
    name = name,
    quantityMilli = quantityMilli,
    unit = unit,
    unitCostPesos = unitCostPesos
)

fun HistoryEntity.toModel(): HistoryEntry = HistoryEntry(
    id = id,
    timestamp = timestamp,
    type = runCatching { HistoryType.valueOf(type) }.getOrDefault(HistoryType.CALCULATION),
    title = title,
    detail = detail
)
