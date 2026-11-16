# Icon categories and sorting

Open `/icons` without needing to select a database. The default order is **Category**, then **Human-readable name**. Click **Index**, **Human-readable name**, or **Category** to toggle ascending/descending order. Category sorting uses the displayed category label in the selected interface language; names within a category remain ascending.

Choose a category and/or search by human-readable name, enum name, asset basename or category label. Search ignores case and accents, including Turkish dotted/dotless I. Filters survive header sorting. **Reset** clears filters and returns to category order.

The index is the original declaration position in `NodeIcon` (`ordinal + 1`), so it does not get renumbered when the table is sorted or filtered. It is a display index, not a persistent database identifier. The existing **ID** and **ID in 16 bit** columns retain their meaning.

## Editing categories

Each entry in `src/main/java/com/turkerozturk/helpers/NodeIcon.java` has a fifth argument:

```java
CT_PYTHON(46, 23, "ct_python", "Python", "programming"),
```

Change that last string to regroup an icon. Current categories are `actions`, `activities`, `animals`, `cherries`, `files`, `flags`, `food`, `formats`, `nature`, `navigation`, `other`, `people`, `places`, `programming`, `status`, `symbols`, `technology`, `transport`, and `weather`.

For a new category, optionally add `icons.category.<value>=<label>` to the default/English/Turkish message bundles. A category without a translation is displayed as written, so adding a translation is not mandatory. Rebuild and restart after editing the enum/resources. No database change is needed.

Categories are browsing metadata: Apple/Tux represent platforms in `technology`, Python/Ruby represent languages in `programming`, while the animal-shaped icons use `animals`. The page's SVG assets, enum constant names, numeric IDs, CTB bit mappings and existing label lookup behavior are unchanged. This change does not regroup the separate node-property icon picker automatically.
