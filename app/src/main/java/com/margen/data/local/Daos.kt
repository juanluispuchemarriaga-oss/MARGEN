package com.margen.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ProductDao {

    @Query("SELECT * FROM products ORDER BY updatedAt DESC")
    abstract fun observeProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    abstract fun observeProduct(id: Long): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE id = :id")
    abstract suspend fun getProduct(id: Long): ProductEntity?

    @Query("SELECT COUNT(*) FROM products")
    abstract suspend fun count(): Int

    @Insert
    abstract suspend fun insertProduct(product: ProductEntity): Long

    @Update
    abstract suspend fun updateProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    abstract suspend fun deleteProduct(id: Long)

    @Query("DELETE FROM products")
    abstract suspend fun deleteAllProducts()

    @Query("SELECT * FROM cost_items ORDER BY id")
    abstract fun observeAllCostItems(): Flow<List<CostItemEntity>>

    @Query("SELECT * FROM cost_items WHERE productId = :productId ORDER BY id")
    abstract fun observeCostItems(productId: Long): Flow<List<CostItemEntity>>

    @Query("SELECT * FROM cost_items WHERE productId = :productId ORDER BY id")
    abstract suspend fun getCostItems(productId: Long): List<CostItemEntity>

    @Insert
    abstract suspend fun insertCostItems(items: List<CostItemEntity>)

    @Query("DELETE FROM cost_items WHERE productId = :productId")
    abstract suspend fun deleteCostItems(productId: Long)

    /** Guarda el producto y reemplaza todos sus costos en una sola transacción. */
    @Transaction
    open suspend fun saveProduct(product: ProductEntity, items: List<CostItemEntity>): Long {
        val id = if (product.id == 0L) {
            insertProduct(product)
        } else {
            updateProduct(product)
            product.id
        }
        deleteCostItems(id)
        insertCostItems(items.map { it.copy(id = 0, productId = id) })
        return id
    }
}

@Dao
interface HistoryDao {

    @Query("SELECT * FROM history ORDER BY timestamp DESC, id DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history ORDER BY timestamp DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<HistoryEntity>>

    @Insert
    suspend fun insert(entry: HistoryEntity): Long

    @Query("DELETE FROM history")
    suspend fun deleteAll()
}
