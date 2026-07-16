package ru.samlib.client.ui.common

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.compose.collectSideEffect
import ru.samlib.client.ui.R

/** Единый результат экранов редактирования раздела (сохранить/удалить/ошибка). */
sealed interface EditSideEffect {
    data object Saved : EditSideEffect

    data object Deleted : EditSideEffect

    data class Error(
        val message: String,
    ) : EditSideEffect
}

/** Стандартная обработка: тост + закрытие экрана на Saved/Deleted, тост на Error. */
@Composable
fun ContainerHost<*, EditSideEffect>.CollectEditEffects(
    onDone: () -> Unit,
    savedMessage: String = stringResource(R.string.common_saved),
    deletedMessage: String = stringResource(R.string.common_deleted),
) {
    val context = LocalContext.current
    collectSideEffect { effect ->
        when (effect) {
            is EditSideEffect.Saved -> {
                Toast.makeText(context, savedMessage, Toast.LENGTH_SHORT).show()
                onDone()
            }

            is EditSideEffect.Deleted -> {
                Toast.makeText(context, deletedMessage, Toast.LENGTH_SHORT).show()
                onDone()
            }

            is EditSideEffect.Error ->
                Toast.makeText(context, effect.message, Toast.LENGTH_LONG).show()
        }
    }
}
