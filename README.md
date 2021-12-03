# Пример испольования кастомной фабрики моделей
Этот пример — сплошное нарушение основных правил программирования Android-приложений. 

Во-первых, 
здесь используется [AndroidViewModel (AVM)](https://developer.android.com/reference/androidx/lifecycle/AndroidViewModel).

Не стоит хранить контекст приложения в модели. Вообще, ничего от приложения не должно пребывать внутри модели. Но соблазн
запихать в модель сохранение данных выше всяких предосторожностей.


Ведь, все мы знаем, что наличие статического экземпляра контекста - зло, поскольку оно может вызвать
утечку памяти!! Однако наличие статического экземпляра приложения - это не так плохо, как вы думаете, 
потому что в запущенном приложении есть только один экземпляр приложения, а значит «всё под контролем»
Следовательно, использование и наличие экземпляра Application в определенном классе в целом не является 
проблемой. Но если экземпляр приложения ссылается на них, это проблема из-за проблемы цикла ссылок.
Вторая проблема — тестирование приложения. В обычном модульном тесте сложно создать контекст приложения.

## Основные моменты
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
3. Модель создаётся при помощи `viewModels {...}`:
```kotlin
    private val testViewModel:TestAndroidViewModel by viewModels<TestAndroidViewModel> {
        TestAndroidViewModelFactory(requireActivity().application, "First")
    }
```
И во втором фрагменте:
```kotlin
    private val testViewModel:TestAndroidViewModel by viewModels<TestAndroidViewModel> {
        TestAndroidViewModelFactory(requireActivity().application, "Second")
    }
```
4. Тогда, внутри модели можно реализовать метод сохранения данных:
```kotlin
    fun store() {
        preferences.edit {
            _regime.value?.let { putInt(name + REGIME, it) }
        }
    }
```
Небольшая проблема в том, что функции обратного вызова жизненного цикла модели `onClear()`, 
равно, как и коллбэки жизненненого цикла фрагмента `onDestroy()` и `onDestroyView()` отрабатывают 
далеко не всегда.

Чтобы эти методы вызывались при завершении приложения, необходимо явно вызывать `finish()`. Всё, что у нас
есть из срабатывающего всегда --- это `onStart()`, `onStop()`, `onPause()`, `onResume()`.

В этом примере будем использовать `onStop()` в (./app/src/main/java/com/grandfatherpikhto/testviewmodels/FirstFragment.kt)[`FirstFragment.kt`], 
(./app/src/main/java/com/grandfatherpikhto/testviewmodels/SecondFragment.kt)[`SecondFragment.kt`]:
```kotlin
    override fun onStop() {
        super.onStop()
        testViewModel.store()
        Log.d(TAG, "onStop()")
    }
```

Собственно, почти всё. 
Осталось инициализировать `Number Wheel Picker` в классе фрагмента (`FirstFragment.kt`, `SecondFragment.kt`):

```kotlin
        binding.apply {
            buttonFirst.setOnClickListener {
                findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
            }
            val regimes = arrayOf<String>("Off", "Tag", "Tail", "Water", "Blink")
            npRegimeFirst.displayedValues = regimes
            npRegimeFirst.minValue = 0
            npRegimeFirst.maxValue = regimes.size - 1
            npRegimeFirst.value = testViewModel.regime.value!!
            npRegimeFirst.setOnValueChangedListener { numberPicker, previous, value ->
                Log.d(TAG, "Regime: $value")
                if(testViewModel.regime.value != value) {
                    testViewModel.changeRegime(value)
                }
            }
            testViewModel.regime.observe(viewLifecycleOwner, { value ->
                if(value != npRegimeFirst.value) {
                    npRegimeFirst.value = value
                }
            })
        }
    }
```

Важный момент: при присваивании данных обязательна проверка неравенства между данными модели
и данными `Wheel Picker`. Если этого не сделать, возникнет закольцованный вызов между данными
модели и данными пикера.