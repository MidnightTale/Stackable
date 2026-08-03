Stack sizes for items that normally do not stack well, including potions, stews, boats, minecarts, saddles, shulker boxes, and music discs.

![Custom item stack sizes](assets/stackable-inventory.png)

Configure stack sizes in `plugins/Stackable/config.yml`:

```yml
require-permission: true

items:
  potion: 8
  saddle: 16
  "*_boat": 4
  "music_disc_*": 16
```

When `require-permission` is enabled, players need `stackable.use`.

Rank-specific stack sizes can be configured under `groups:` and granted with:

```text
stackable.group.<name>
```

Example:

```yml
groups:
  vip:
    items:
      potion: 16
      saddle: 32
```

Wildcard rules can match every boat, minecart, colored shulker box, or music disc.

Run `/stackablereload` after changing the config.

Permissions: `stackable.use`, `stackable.reload`, `stackable.admin`, and `stackable.group.<name>`.
