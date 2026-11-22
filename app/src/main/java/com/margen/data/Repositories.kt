package com.margen.data

import androidx.room.withTransaction
import com.margen.core.MoneyFormat
import com.margen.data.local.HistoryDao
import com.margen.data.local.HistoryEntity
import com.margen.data.local.MargenDatabase
import com.margen.data.local.toEntity
import com.margen.data.local.toModel
import com.margen.domain.model.CostItem
import com.margen.domain.model.HistoryEntry
import com.margen.domain.model.HistoryType
import com.margen.domain.model.Product
import com.margen.domain.model.ProductWithCosts
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class HistoryRepository(private val dao: HistoryDao) {

    val entries: Flow<List<HistoryEntry>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    fun observeRecent(limit: Int): Flow<List<HistoryEntry>> =
        dao.observeRecent(limit).map { list -> list.map { it.toModel() } }

    suspend fun log(type: HistoryType, title: String, detail: String) {
        dao.insert(
            HistoryEntity(
                timestamp = System.currentTimeMillis(),
                type = type.name,
                title = title,
                detail = detail
            )
        )
    }
}

class ProductRepository(private val db: MargenDatabase) {

    private val productDao = db.productDao()
    private val historyDao = db.historyDao()

    val products: Flow<List<ProductWithCosts>> =
        combine(productDao.observeProducts(), productDao.observeAllCostItems()) { products, items ->
            val byProduct = items.groupBy { it.productId }
            products.map { p ->
                ProductWithCosts(
                    product = p.toModel(),
                    items = byProduct[p.id].orEmpty().map { it.toModel() }
                )
            }
        }

    fun observeProduct(id: Long): Flow<ProductWithCosts?> =
        combine(productDao.observeProduct(id), productDao.observeCostItems(id)) { product, items ->
            product?.let { ProductWithCosts(it.toModel(), items.map { item -> item.toModel() }) }
        }

    suspend fun getProduct(id: Long): ProductWithCosts? {
        val product = productDao.getProduct(id) ?: return null
        val items = productDao.getCostItems(id).map { it.toModel() }
        return ProductWithCosts(product.toModel(), items)
    }

    suspend fun count(): Int = productDao.count()

    /** Crea (id = 0) o actualiza un producto con sus costos y deja constancia en el historial. */
    suspend fun save(product: Product, items: List<CostItem>): Long = db.withTransaction {
        val now = System.currentTimeMillis()
        val isNew = product.id == 0L
        val entity = product.toEntity().copy(
            createdAt = if (isNew) now else product.createdAt,
            updatedAt = now
        )
        val id = productDao.saveProduct(entity, items.map { it.toEntity() })
        val total = items.sumOf { it.totalPesos }
        historyDao.insert(
            HistoryEntity(
                timestamp = now,
                type = (if (isNew) HistoryType.PRODUCT_CREATED else HistoryType.PRODUCT_UPDATED).name,
                title = product.name,
                detail = buildDetail(total, product.salePricePesos)
            )
        )
        id
    }

    suspend fun delete(id: Long) {
        db.withTransaction {
            val product = productDao.getProduct(id) ?: return@withTransaction
            productDao.deleteProduct(id)
            historyDao.insert(
                HistoryEntity(
                    timestamp = System.currentTimeMillis(),
                    type = HistoryType.PRODUCT_DELETED.name,
                    title = product.name,
                    detail = "El producto y sus costos fueron eliminados."
                )
            )
        }
    }

    /** Borra productos, costos e historial. */
    suspend fun clearAllData() {
        db.withTransaction {
            productDao.deleteAllProducts()
            historyDao.deleteAll()
        }
    }

    private fun buildDetail(totalCost: Long, salePrice: Long?): String {
        val cost = "Costo total: ${MoneyFormat.cop(totalCost)}"
        return if (salePrice != null) "$cost · Precio de venta: ${MoneyFormat.cop(salePrice)}" else cost
    }
}
