# Profile account API

The `secura_profile_account` table is separate from the existing profile account
JSON; neither is migrated or synchronized. Rows are identified by `profile_id`
and `apartment_id`. Hibernate creates the table when schema updates are enabled;
deployments managing their own schema must create it before using the endpoints.

Both endpoints require the existing bearer authentication and accept JSON:

* `POST /profile/getProfileAccountDetails`: `{"profileId":"P1","apartmentId":"A1"}`.
  Returns the account, or HTTP 404 if absent.
* `POST /profile/updateProfileAccountDetails`: supply `profileId`, `apartmentId`,
  `position`, `role`, `jobeposition`, `flatId`, and `status`. This creates or
  replaces the mutable fields of the row; omitted mutable fields become null.
  The response is the saved account. Audit users come from the authenticated
  token subject, and timestamps are managed by Hibernate.

Both IDs must be nonblank. The table stores one flat ID per profile/apartment
row; it does not represent the multiple flats in the existing JSON.
