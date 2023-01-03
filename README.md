![Back to the Fabric!](.pretty_readme/bttf.png)

This is the BttF port of the [DefaultResources](https://github.com/lukebemish/defaultresources)
library for [Quilt](https://quiltmc.org)

---

# what it does

I honestly am not sure myself either. Despite porting the code, I still struggle to understand it.
I wish Luke would add more documentation about his work, but I guess we have to make do with what we have.

---

# How to import

### To Maven

**Step 1.** Add the JitPack repository to your build file
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

**Step 2.** Add the dependency (adding the version of your needs)
```xml
<dependencies>
    <dependency>
        <groupId>com.github.Back-to-the-Fabric</groupId>
        <artifactId>DefaultResources</artifactId>
        <version>...</version>
    </dependency>
</dependencies>
```

### To Gradle

**Step 1.** Add the JitPack repository to your build file
```groovy
repositories {
    maven {
        name "JitPack"
        url "https://jitpack.io"
    }
}
```

**Step 2.** Add the dependency (adding the version of your needs)
```groovy
dependencies {
    implementation "com.github.Back-to-the-Fabric:DefaultResources:<version>"
}
```