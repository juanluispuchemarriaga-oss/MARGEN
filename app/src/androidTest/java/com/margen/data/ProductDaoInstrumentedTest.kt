package com.margen.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.margen.data.local.MargenDatabase
import com.margen.domain.model.CostCategory
import com.margen.domain.model.CostItem
import com.margen.domain.model.Product
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prueba de persistencia con base de datos en memoria. Requiere emulador o teléfono
 * (./gradlew connectedDebugAndroidTest). No corre en GitHub Actions. DATOS DE PRUEBA ficticios.
 */
@RunWith(AndroidJUnit4::class)
class ProductDaoInstrumentedTest {

    private lateinit var db: MargenDatabase
    private lateinit var repo: ProductRepository

    @Before fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, MargenDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = ProductRepository(db)
    }

    @After fun tearDown() = db.close()

    private fun sampleItems() = listOf(
        CostItem(category = CostCategory.INGREDIENT, name = "PRUEBA Harina", quantityMilli = 1_500, unit = "kg", unitCostPesos = 4_200),
        CostItem(category = CostCategory.PACKAGING, name = "PRUEBA Caja", quantityMilli = 1_000, unit = "unidad", unitCostPesos = 1_500)
    )

    @Test fun guardaYLeeProductoConCostos() = runBlocking {
        val id = repo.save(Product(name = "PRUEBA Torta", description = "", salePricePesos = 19_000), sampleItems())
        val loaded = repo.getProduct(id)!!
        assertEquals("PRUEBA Torta", loaded.product.name)
        assertEquals(19_000L, loaded.product.salePricePesos)
        assertEquals(2, loaded.items.size)
        assertEquals(7_800L, loaded.totalCostPesos)
    }

    @Test fun editarReemplazaLosCostos() = runBlocking {
        val id = repo.save(Product(name = "PRUEBA A", description = "", salePricePesos = null), sampleItems())
        val first = repo.getProduct(id)!!
        repo.save(first.product.copy(name = "PRUEBA B"), sampleItems().take(1))
        val edited = repo.getProduct(id)!!
        assertEquals("PRUEBA B", edited.product.name)
        assertEquals(1, edited.items.size)
        assertNull(edited.product.salePricePesos)
    }

    @Test fun eliminarBorraProductoYCostos() = runBlocking {
        val id = repo.save(Product(name = "PRUEBA C", description = "", salePricePesos = 1_000), sampleItems())
        repo.delete(id)
        assertNull(repo.getProduct(id))
        assertEquals(0, db.productDao().getCostItems(id).size)
    }

    @Test fun borrarTodoLimpiaProductosEHistorial() = runBlocking {
        repo.save(Product(name = "PRUEBA D", description = "", salePricePesos = 1_000), sampleItems())
        repo.clearAllData()
        assertEquals(0, repo.count())
    }
}
