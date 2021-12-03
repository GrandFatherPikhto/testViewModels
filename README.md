# Пример испольования кастомной фабрики моделей
Этот пример — сплошное нарушение основных правил программирования Android-приложений. Во-первых, 
здесь используется [https://developer.android.com/reference/androidx/lifecycle/AndroidViewModel](AndroidViewModel (AVM)).
Не стоит хранить контекст приложения в модели. Вообще, ничего от приложения не должно пребывать внутри модели. Но соблазн
запихать в модель сохранение данных выше всяких предосторожностей.
Ведь, все мы знаем, что наличие статического экземпляра контекста - зло, поскольку оно может вызвать
утечку памяти!! Однако наличие статического экземпляра приложения - это не так плохо, как вы думаете, 
потому что в запущенном приложении есть только один экземпляр приложения, а значит «всё под контролем»
Следовательно, использование и наличие экземпляра Application в определенном классе в целом не является 
проблемой. Но если экземпляр приложения ссылается на них, это проблема из-за проблемы цикла ссылок.
Вторая проблема — тестирование приложения. В обычном модульном тесте сложно создать контекст приложения.
## Основные участки
1. Для получения контекста приложения в Fragment, используем `requireActivity().application`.
2. Фабрика моделей используется в своём простейшем «изводе»
Для `ViewModel` дополнительный параметр — name. Название для сохранения в `SharedPreferences` уникального значения
```kotlin
class TestViewModelFactory(private val name:String): ViewModelProvider.NewInstanceFactory() {
    companion object {
        const val TAG:String = "TestViewModelFactory"
    }
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        Log.d(TAG, "$name")
        return TestViewModel(name) as T
    }
}
```

Для `AndroidViewModel` дополнительный параметр, также, `name`:
```kotlin
class TestAndroidViewModelFactory(private val application: Application, private val name:String): ViewModelProvider.NewInstanceFactory() {
    companion object {
        const val TAG:String = "TestAndroidViewModelFactory"
    }
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        Log.d(TAG, "Create model instance: $name")
        return TestAndroidViewModel(application, name) as T
    }
}
```
