# Overlay

In this folder you can find overlay example which you need to add to your project.

```xml
<page-packager
    jcr:primaryType="nt:unstructured"
    sling:resourceType="granite/ui/components/coral/foundation/button"
    granite:class="js-page-packager"
    granite:title="i18.title.key"
    text="i18.title.key">
    <granite:data
        jcr:primaryType="nt:unstructured"
        path="${requestPathInfo.suffix}.pagepackager.zip"/>
</page-packager>
```

You can use any i18n key you want or just add plain title here.

```xml
 granite:class="js-page-packager"
```
This class is required to init button.
