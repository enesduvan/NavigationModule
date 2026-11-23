package com.enesduvan.navigationmodule

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay

/**
 * Uygulama içi ekran geçişlerini ve geri yığınını (back stack) tip güvenli (Type-Safe) şekilde yöneten ana sınıf.
 *
 * @property backStack Aktif ekran geçmişini tutan durum listesi.
 */
@Stable
class AppNavigator(
    val backStack: NavBackStack<NavKey>
) {
    /**
     * Şu anda ekranda açık olan en üstteki rotayı döndürür. Yığın boşsa `null` döner.
     */
    val currentRoute: NavKey?
        get() = backStack.lastOrNull()

    /**
     * Geri dönülebilecek bir önceki ekranın olup olmadığını belirtir.
     * Ana (kök) ekrandayken `false` döner.
     */
    val canGoBack: Boolean
        get() = backStack.size > 1

    /**
     * Belirtilen yeni ekrana geçiş yapar ve mevcut ekranı geçmişte (back stack) saklar.
     *
     * @param route Gidilecek hedef ekran rotası ([NavKey]).
     */
    fun navigateTo(route: NavKey) {
        backStack.add(route)
    }

    /**
     * Hedef ekran zaten en üstte açıksa tekrar aynı ekranı yığına eklemez.
     * Alt menü (Bottom Navigation) geçişlerinde veya butona çift tıklanma durumlarında kullanılır.
     *
     * @param route Gidilecek hedef ekran rotası ([NavKey]).
     */
    fun navigateSingleTop(route: NavKey) {
        if (backStack.lastOrNull() != route) {
            backStack.add(route)
        }
    }

    /**
     * Bir önceki ekrana geri döner.
     * Eğer kullanıcı zaten kök (ilk) ekrandaysa işlem yapmaz ve uygulamanın çökmesini engeller.
     *
     * @return Geri dönüş işlemi başarıyla gerçekleştiyse `true`, kök ekranda olduğu için dönülemediyse `false`.
     */
    fun navigateBack(): Boolean {
        return if (backStack.size > 1) {
            backStack.removeLastOrNull()
            true
        } else {
            false
        }
    }

    /**
     * Tüm ekran geçmişini (back stack) tamamen temizler ve belirtilen ekranı yeni başlangıç (kök) ekranı yapar.
     * Genellikle Splash veya Giriş (Login) ekranından Ana Ekrana geçerken geri tuşuyla eski ekrana dönülmemesi için kullanılır.
     *
     * @param route Yeni kök ekran olarak ayarlanacak rota ([NavKey]).
     */
    fun navigateAndClear(route: NavKey) {
        backStack.clear()
        backStack.add(route)
    }

    /**
     * Yığındaki belirli bir hedef ekrana ulaşana kadar aradaki tüm ekranları kapatarak geri sarar.
     * Örneğin: Sepet -> Adres -> Ödeme adımlarından sonra doğrudan Ana Ekrana dönmek için kullanılır.
     *
     * @param targetRoute Geri dönülmek istenen hedef ekran rotası ([NavKey]).
     * @param inclusive `true` verilirse hedef ekranın kendisi de yığından silinir; `false` verilirse hedef ekranda durulur.
     */
    fun popUpTo(targetRoute: NavKey, inclusive: Boolean = false) {
        val index = backStack.indexOfLast { it == targetRoute }
        if (index != -1) {
            val removeFrom = if (inclusive) index else index + 1
            while (backStack.size > removeFrom) {
                backStack.removeLastOrNull()
            }
        }
    }
}

/**
 * Composable yaşam döngüsüne (recomposition ve ekran döndürme) uyumlu bir [AppNavigator] örneği oluşturur ve hatırlar.
 *
 * @param startDestination Uygulama ilk açıldığında gösterilecek başlangıç ekranı.
 * @return Yapılandırılmış [AppNavigator] yöneticisi.
 */
@Composable
fun rememberAppNavigator(
    startDestination: NavKey
): AppNavigator {
    val backStack = rememberNavBackStack(startDestination)
    return remember(backStack) {
        AppNavigator(backStack)
    }
}

/**
 * Standart sağdan sola kayma (slide) animasyonlarına ve otomatik sistem geri tuşu yönetimine sahip hazır navigasyon kapsayıcısı.
 *
 * @param navigator Ekran geçişlerini kontrol eden [AppNavigator] örneği.
 * @param modifier Kapsayıcıya uygulanacak Jetpack Compose [Modifier] ayarları.
 * @param animationDurationMillis Sayfa geçiş animasyonlarının milisaniye cinsinden süresi (Varsayılan: 300 ms).
 * @param builder Hangi rotanın hangi ekranı çizeceğini tanımlayan `entry<Rota> { ... }` bloğu.
 */
@Composable
fun ForgeNavHost(
    navigator: AppNavigator,
    modifier: Modifier = Modifier,
    animationDurationMillis: Int = 300,
    builder: EntryProviderScope<NavKey>.() -> Unit
) {
    NavDisplay(
        backStack = navigator.backStack,
        modifier = modifier,
        onBack = { navigator.navigateBack() },
        transitionSpec = {
            (slideInHorizontally(animationSpec = tween(animationDurationMillis)) { it } + fadeIn()) togetherWith
                    (slideOutHorizontally(animationSpec = tween(animationDurationMillis)) { -it / 3 } + fadeOut())
        },
        popTransitionSpec = {
            (slideInHorizontally(animationSpec = tween(animationDurationMillis)) { -it / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally(animationSpec = tween(animationDurationMillis)) { it } + fadeOut())
        },
        entryProvider = entryProvider {
            builder()
        }
    )
}