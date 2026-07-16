package ru.samlib.client.domain.usecase

import ru.samlib.client.domain.repository.FileDownloadGateway
import ru.samlib.client.domain.samlib.SamlibLinkFormat
import ru.samlib.client.domain.samlib.SamlibUrls

/** Скачивание FB2-архива книги samlib через системный менеджер загрузок. */
class DownloadFb2UseCase(
    private val gateway: FileDownloadGateway,
) : UseCase<DownloadFb2UseCase.Params, Unit>() {
    data class Params(
        val link: String,
        val title: String?,
    )

    /** По ссылке книги ("/x/xxx/kniga.shtml") строит URL fb2.zip и ставит загрузку в очередь. */
    override suspend fun execute(params: Params) {
        val fb2 = SamlibUrls.BASE + params.link.replace(SamlibLinkFormat.WORK_SUFFIX, ".fb2.zip")
        val name = fb2.substringAfterLast('/')
        gateway.enqueue(fb2, params.title?.ifBlank { name } ?: name)
    }
}
