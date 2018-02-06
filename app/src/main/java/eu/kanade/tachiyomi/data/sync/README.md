# Tachiyomi Sync Protocol
#### Definitions
Some quick definitions of the words used in this doc
- **Generator:** The device generating the report
- **Receiver:** The device receiving the report

## Sync process
![Sync diagram](https://user-images.githubusercontent.com/9571936/35840375-de6e15f4-0ac3-11e8-8d8c-140bb27cf7c8.png)
<!--- Mermaid source:
sequenceDiagram
participant Client
participant Server
Note over Client,Server: Client ensures auth token is valid with server
Note over Client: Generate client sync report
Client ->> Server: Send client sync report
Note over Server: Apply client sync report
Note over Server: Generate server sync report
Note over Server: Correct server timestamps
Note over Server: Take server snapshots
Note over Server: Update server lastSync times
Server ->> Client: Send server sync report
Note over Client: Apply server sync report
Note over Client: Correct client timestamps
Note over Client: Take client snapshots
Note over Client: Update client lastSync time
-->

## Sync reports
Sync reports describe the changes that have occurred on the **generator** since the last time a report was generated for this **reciever**.

Sync reports include:
- **Device ID:** A unique UUID used to identify the **generator**
- **Entities:** A list of all data involved in the sync report
- **From datetime:** The earliest datetime that the information in this sync report covers
- **To datetime:** The latest datetime that the information in this sync report covers (usually the datetime the report is generated)

### Generating sync reports
1. Fill in the sync report's device ID along with it's from and to datetimes. Generate a new (random) device ID and store it locally if the device currently has no device ID.
2. Find all changed entities between the from and to datetimes and include them in the report:
    - If entity depends on other entities, include them too!
        - Example: A chapter was marked as 'read'. This chapter 'depends' on it's manga so include the manga in the report as well.
    - Include the datetimes for when each change occurred
    - If a change occurred to the same property of the same entity more than once, include only the latest change:
        - Example: If a chapter was marked as 'read', and then 'unread' and then 'read' again, only include the fact that the chapter was marked as 'read' and when it was last done so.
    - If you are unsure of when the change occurred, just assume it occurred on the **from datetime**. Only do this as a last resort.

#### Report change format
Reports are subdivided into **entities**:
Entities represent a meaningful piece of data.

##### Currently defined entities
- manga
- chapter
- history entry
- track
- manga --> category binding
- category
- source

##### Entity format
- **ID:** A unique ID allocated for this entity. Each entity in the same report must have a different ID.
- **Identifiers:** Used to identify this entity on the **receiver**. Also used to build a skeleton UI if this entity is not present in the **reciever**'s database and must be inserted as a result. Example identifiers for a manga:
    - **Manga URL and source**: Can be used to uniquely identify a manga
    - **Manga title and thumbnail URL**: Will be used to build a skeleton UI if the manga does not exist on the **reciever** (so we don't have to download the metadata during the sync).
- **Change fields:** A list of properties of this entity that have changed. Format for each change field:
    - **Name:** The name of the property that changed
    - **New value:** The latest value of this property
    - **Datetime:** The latest datetime this property was changed

    Example change fields for manga:
    - **Favorited:** Whether or not this manga is favorited
    - **Viewer:** The viewer used to view this manga
- **Deleted:** An optional boolean field representing whether or not this entity was deleted. Most entries do not have to handle deletions (such as manga and chapters). Some examples of entities that do require deletion handling include tracks and categories.

##### References
Some entities must refer to other sync entities. To avoid serializing multiple copies of the same entity, use references instead. You can do this by first including the dependency in the report and then storing the dependency's ID in the entity.

### Applying a sync report
1. Go through all entities included in the report, matching them against the corresponding data model in the database. If the entity is not in the database, create a new instance of the database data model copying over any **identifiers** from the entity.
2. Go through all change fields in the entities, applying their respective changes to the database data model if the changes are newer than any changes made to the same entity and property in the local database.
    - Flag all entities changed during this process as those are the entities that must be updated/inserted in the database.
    - Track any changes you made here, you will need them when [correcting timestamps](#correcting-timestamps)
    - The timestamps of any changes that are applied in this step are recorded as `System.currentTimeMillis()` (ignore the datetime in specified in the change field)
3. Insert into/update the database with all entities that have been flagged in the previous step and any entities that were never in the database to begin with.

### Taking snapshots
Items that are able to be deleted cannot have their changes tracked. Changes for these items are instead calculated by saving the state of the items after a sync is complete and comparing them to the current state while syncing.

These items are listed below along with where their snapshots are stored:
- Categories: Snapshots stored in file
- Manga categories: Snapshots stored in DB
- Tracks: Snapshots stored in file

#### Why can deletions not be tracked?
- When a deletion occurs, the change tracker must record the state of the deleted item.
  - This requires a separate change tracking table for each object
  - Queries and cursor mappings must be written for these tables
- If an item is deleted and then inserted again, the change tracker must be able to track this.
  - Deleted objects have no IDs making it difficult to find newly inserted copies of the deleted object if the deleted object was re-inserted

### Correcting timestamps
Remember how in step 2 in [applying a sync report](#applying-a-sync-report) all the timestamps for changes were recorded as `System.currentTimeMillis()`?

Those timestamps are wrong, the actual correct time for those changes are present in the change fields.

Now is the time we fix the recorded timestamps to match the ones in the change fields.

Why did we not just directly record the datetime in the change fields instead of deferring it to a separate step? We had to ensure that the changes applied in the sync report are not included in the sync report we generate to send back to the device. Since the generated sync report only includes changes that are made before sync started, we can exclude changes from the sync report by pretending they occurred after sync started. Once we have generated the sync report for the client, we can correct the timestamps.

## Example sync report
```
{
  "deviceId":"c38064cb_bf4b_4fb0_8ee0_268057f2a03e",
  "entities":[
    {
      "t":"Chapter",
      "v":{
        "chapterNum":20,
        "lastPageRead":{
          "date":1517889504570,
          "value":2
        },
        "manga":{
          "targetId":1
        },
        "name":"Hachinan tte, Sore wa Nai Deshou! 20",
        "sourceOrder":4,
        "url":"/manga/hachinan_tte_sore_wa_nai_deshou/c020/",
        "syncId":2
      }
    },
    {
      "t":"History",
      "v":{
        "chapter":{
          "targetId":2
        },
        "lastRead":{
          "date":1517889504588,
          "value":1517889504584
        },
        "syncId":3
      }
    },
    {
      "t":"Manga",
      "v":{
        "name":"Hachinan tte, Sore wa Nai Deshou!",
        "source":{
          "targetId":0
        },
        "thumbnailUrl":"https://mhcdn.secure.footprint.net/store/manga/20863/cover.jpg?token=259ed1230ee4fda83cd8498e28410516dd538824&ttl=1515758400&v=1515348578",
        "url":"/manga/hachinan_tte_sore_wa_nai_deshou/",
        "syncId":1
      }
    },
    {
      "t":"Source",
      "v":{
        "id":2,
        "name":"Mangahere",
        "syncId":0
      }
    }
  ],
  "from":1517889477512,
  "to":1517889506578
}
```
#### Things to take note of:
- Between this sync and the last sync, I read one page in a chapter
- Notice how the `lastPageRead` field specifies a `date` as well as a `value`. The `date` represents the time that field was last changed and the `value` represents the current value of the field.
- Notice how the chapter refers the manga, so the manga is included in the report and referenced by the chapter with: `"targetId":1`
