# Changelog

## 0.1.0

Inited :)

### 0.1.1

* `Versions`:
    * `kotlinx.serialization`: `1.0.0-RC2` -> `1.0.0`
* `Pagination`
    * `Common`
        * Function `calculatePage` for calculating page based on size of page and index of first element
        * Extension `List#createPaginationResult` which use index of first element to calculate current page and other
        info in `PaginationResult` object
        * Factory `emptyPaginationResult` for empty `PaginationResult`
        * Extensions `paginate` for creating of `PaginationResult` which use as source one of next types:
            * `Iterable`
            * `List`
            * `Set`
* `Repos`
    * `Common`
        * Interfaces related to `OneToManyKeyValueRepo` were renamed with convenience to `Read`/`Write` modifier before name
            * All subclasses were renamed
        * Interfaces related to `StandartKeyValueRepo` were renamed with convenience to `Read`/`Write` modifier before name
            * All subclasses were renamed
        * Extensions `doForAll` and `getAll` were added for all current types of repos:
            * `ReadStandardCRUDRepo`
            * `ReadStandardKeyValueRepo`
            * `ReadOneToManyKeyValueRepo`
        * `ReadStandardKeyValueRepo` methods `values` and `keys` now have default value for `reversed` parameter `false`
        * New `Flow`'s in `WriteOneToManyKeyValueRepo`:
            * `onNewValue`
            * `onValueRemoved`
            * `onDataCleared`
        * New function `ReadStandardCRUDRepo#count`
    * `In Memory`
        * Package has been created:) you can connect it via `implementation "dev.inmo:micro_utils.repos.inmemory"`
        * `MapCRUDRepo` class as implementation of `StandardCRUDRepo` on top of `MutableMap` has been added
        * `MapKeyValueRepo` class as implementation of `StandardKeyValueRepo` on top of `MutableMap` has been added
        * `MapOneToManyKeyValueRepo` class as implementation of `OneToManyKeyValueRepo` on top of `MutableMap` has been added