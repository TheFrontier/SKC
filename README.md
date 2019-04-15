# Sponge Kotlin annotation Commands

A Kotlin-based annotation command system for the Sponge platform.

Commands shouldn't be verbose to set up, and this library simplifies the work into simply declaring functions.

## A Real World Example

```kotlin
@Command("resident", "res")
object CommandResident {

    @Command("friend")
    object SubFriend {
    
        @Command("add")
        fun add(@Source src: CommandSource, @Source srcResident: Resident, resident: Resident) {
            transaction(DB) {
                srcResident.friends = SizedCollection(srcResident.friends + resident)
                src.sendMessage("Added a friend: ".green() + resident.name.white())
            }
        }
    }
}
```

## Gradle (Kotlin DSL)

```kotlin
repositories {
    maven {
        setUrl("https://jitpack.io")
    }
}

dependencies {
    implementation("com.github.TheFrontier:SKC:<current version>")
}
```