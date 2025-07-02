package otus.homework.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class CatsPresenter(
    private val catsService: CatsService,
    private val imageService: ImageService,
    private val presenterScope: CoroutineScope,
    private val showToast: (String) -> Unit
) {

    private var _catsView: ICatsView? = null

    fun onInitComplete() {
        presenterScope.launch {
            val getFact  = async { processGet { catsService.getCatFact() } }
            val getImage = async { processGet { imageService.getImage() }?.firstOrNull() }

            val fact = getFact.await() ?: return@launch
            val image = getImage.await() ?: return@launch

            _catsView?.populate(Model(fact = fact, imageUrl = image.url))
        }
    }

    private suspend fun <T> processGet(getData: suspend () -> T): T? {
        return try {
            getData.invoke()
        } catch (e: java.net.SocketTimeoutException) {
            showToast.invoke("Не удалось получить ответ от сервера")
            null
        } catch (e: Exception) {
            CrashMonitor.trackWarning()
            showToast.invoke(e.message.toString())
            null
        }
    }

    fun attachView(catsView: ICatsView) {
        _catsView = catsView
    }

    fun detachView() {
        _catsView = null
        presenterScope.cancel()
    }
}