# Test quantity — to-be

## Summary and as-is comparison

| Metric | As-is | To-be |
|---|---:|---:|
| Java test-source files | 21 | 99 |
| Created test declarations, including commented methods | 109 | 694 |
| Implemented declarations, excluding empty placeholders | 106 | 691 |
| Uncommented test declarations | 102 | 687 |
| Commented/inactive declarations | 7 | 7 |
| Executed cases (includes parameterized invocations) | 102 | 752 |
| Failures / errors / skipped | 0 / 0 / 0 | 0 / 0 / 0 |

Source declarations and executed cases are different units. A parameterized method counts once in the source inventory and once per input in Surefire. Commented methods are source-history placeholders, not executable tests. Implemented means a non-empty body, not proof of useful assertions. The lexical inventory recognizes JUnit test annotations and excludes comments for live declaration counts.

There are 23 parameterized declarations, 0 live disabled/ignored annotations and 3 empty placeholders in the source inventory.

## Execution groups

| Group | Executed cases |
|---|---:|
| *Test / *Tests filename selection | 601 |
| *IT filename selection | 151 |
| Bibliographic package (subset of the above) | 60 |

51 cases belong to classes directly annotated with SpringBootTest, DataJpaTest or WebMvcTest. This reproduces the as-is annotation proxy, but is not a complete integration classification. Non-Spring tests can execute real collaborations, and some classes mix test boundaries. Filename groups indicate CI selection only.

The full suite is the union of `surefire-reports-global` and `surefire-reports-notification-it`. Delivery tests are excluded from the first selection and run separately against local receivers. The three quality-sample repetitions and PIT runs are not added again.

## Per-source-class inventory

| Class | Live declarations | Commented | Empty placeholders | Executed cases | Spring context annotation |
|---|---:|---:|---:|---:|---|
| [PsoftG1ApplicationIT](../../../../src/test/java/pt/psoft/g1/psoftg1/PsoftG1ApplicationIT.java) | 1 | 0 | 0 | 1 | Yes |
| [TestAuthApi](../../../../src/test/java/pt/psoft/g1/psoftg1/auth/api/TestAuthApi.java) | 0 | 4 | 0 | 0 | No |
| [AuthorApiComponentIT](../../../../src/test/java/pt/psoft/g1/psoftg1/authormanagement/api/AuthorApiComponentIT.java) | 17 | 0 | 0 | 17 | No |
| [AuthorControllerTest](../../../../src/test/java/pt/psoft/g1/psoftg1/authormanagement/api/AuthorControllerTest.java) | 26 | 0 | 0 | 26 | No |
| [AuthorViewMapperTest](../../../../src/test/java/pt/psoft/g1/psoftg1/authormanagement/api/AuthorViewMapperTest.java) | 5 | 0 | 0 | 5 | No |
| [AuthorTest](../../../../src/test/java/pt/psoft/g1/psoftg1/authormanagement/model/AuthorTest.java) | 14 | 0 | 0 | 14 | No |
| [BioTest](../../../../src/test/java/pt/psoft/g1/psoftg1/authormanagement/model/BioTest.java) | 8 | 0 | 0 | 8 | No |
| [AuthorRepositoryIntegrationIT](../../../../src/test/java/pt/psoft/g1/psoftg1/authormanagement/repository/AuthorRepositoryIntegrationIT.java) | 7 | 0 | 0 | 7 | Yes |
| [AuthorServiceImplTest](../../../../src/test/java/pt/psoft/g1/psoftg1/authormanagement/services/AuthorServiceImplTest.java) | 19 | 0 | 0 | 19 | No |
| [AuthorCorrespondenceTest](../../../../src/test/java/pt/psoft/g1/psoftg1/bibliographicmanagement/AuthorCorrespondenceTest.java) | 3 | 0 | 0 | 3 | No |
| [BibliographicApiIT](../../../../src/test/java/pt/psoft/g1/psoftg1/bibliographicmanagement/BibliographicApiIT.java) | 3 | 0 | 0 | 3 | Yes |
| [BibliographicCollaborationIT](../../../../src/test/java/pt/psoft/g1/psoftg1/bibliographicmanagement/BibliographicCollaborationIT.java) | 1 | 0 | 0 | 1 | No |
| [BibliographicConfigurationIT](../../../../src/test/java/pt/psoft/g1/psoftg1/bibliographicmanagement/BibliographicConfigurationIT.java) | 3 | 0 | 0 | 3 | No |
| [BibliographicCoreTest](../../../../src/test/java/pt/psoft/g1/psoftg1/bibliographicmanagement/BibliographicCoreTest.java) | 9 | 0 | 0 | 15 | No |
| [BibliographicRegistrationServiceTest](../../../../src/test/java/pt/psoft/g1/psoftg1/bibliographicmanagement/BibliographicRegistrationServiceTest.java) | 7 | 0 | 0 | 7 | No |
| [CatalogueMatcherTest](../../../../src/test/java/pt/psoft/g1/psoftg1/bibliographicmanagement/CatalogueMatcherTest.java) | 6 | 0 | 0 | 6 | No |
| [OpenLibraryDescriptionIT](../../../../src/test/java/pt/psoft/g1/psoftg1/bibliographicmanagement/OpenLibraryDescriptionIT.java) | 1 | 0 | 0 | 1 | No |
| [ProviderAdapterTest](../../../../src/test/java/pt/psoft/g1/psoftg1/bibliographicmanagement/ProviderAdapterTest.java) | 7 | 0 | 0 | 7 | No |
| [ProviderMappingTest](../../../../src/test/java/pt/psoft/g1/psoftg1/bibliographicmanagement/ProviderMappingTest.java) | 6 | 0 | 0 | 14 | No |
| [BookApiComponentIT](../../../../src/test/java/pt/psoft/g1/psoftg1/bookmanagement/api/BookApiComponentIT.java) | 14 | 0 | 0 | 14 | No |
| [BookControllerTest](../../../../src/test/java/pt/psoft/g1/psoftg1/bookmanagement/api/BookControllerTest.java) | 21 | 0 | 0 | 21 | No |
| [BookViewMapperTest](../../../../src/test/java/pt/psoft/g1/psoftg1/bookmanagement/api/BookViewMapperTest.java) | 5 | 0 | 0 | 5 | No |
| [BookTest](../../../../src/test/java/pt/psoft/g1/psoftg1/bookmanagement/model/BookTest.java) | 14 | 0 | 0 | 14 | No |
| [DescriptionTest](../../../../src/test/java/pt/psoft/g1/psoftg1/bookmanagement/model/DescriptionTest.java) | 5 | 0 | 0 | 5 | No |
| [IsbnTest](../../../../src/test/java/pt/psoft/g1/psoftg1/bookmanagement/model/IsbnTest.java) | 11 | 0 | 0 | 11 | No |
| [TitleTest](../../../../src/test/java/pt/psoft/g1/psoftg1/bookmanagement/model/TitleTest.java) | 8 | 0 | 0 | 8 | No |
| [BookRepositoryIntegrationIT](../../../../src/test/java/pt/psoft/g1/psoftg1/bookmanagement/repositories/BookRepositoryIntegrationIT.java) | 10 | 0 | 0 | 10 | Yes |
| [BookServiceImplTest](../../../../src/test/java/pt/psoft/g1/psoftg1/bookmanagement/services/BookServiceImplTest.java) | 26 | 0 | 0 | 26 | No |
| [BootstrapperIntegrationTest](../../../../src/test/java/pt/psoft/g1/psoftg1/bootstrapping/BootstrapperIntegrationTest.java) | 2 | 0 | 0 | 2 | Yes |
| [LendingParametersConfigTest](../../../../src/test/java/pt/psoft/g1/psoftg1/configuration/LendingParametersConfigTest.java) | 6 | 0 | 0 | 6 | No |
| [LendingPoliciesConfigTest](../../../../src/test/java/pt/psoft/g1/psoftg1/configuration/LendingPoliciesConfigTest.java) | 4 | 0 | 0 | 4 | No |
| [NotificationChannelsConfigTest](../../../../src/test/java/pt/psoft/g1/psoftg1/configuration/NotificationChannelsConfigTest.java) | 14 | 0 | 0 | 20 | No |
| [GenreApiComponentIT](../../../../src/test/java/pt/psoft/g1/psoftg1/genremanagement/api/GenreApiComponentIT.java) | 7 | 0 | 0 | 10 | No |
| [GenreControllerTest](../../../../src/test/java/pt/psoft/g1/psoftg1/genremanagement/api/GenreControllerTest.java) | 5 | 0 | 0 | 5 | No |
| [GenreCrudIntegrationIT](../../../../src/test/java/pt/psoft/g1/psoftg1/genremanagement/api/GenreCrudIntegrationIT.java) | 5 | 0 | 0 | 5 | Yes |
| [GenreViewMapperCollaborationIT](../../../../src/test/java/pt/psoft/g1/psoftg1/genremanagement/api/GenreViewMapperCollaborationIT.java) | 2 | 0 | 0 | 2 | No |
| [GenreBoundaryTest](../../../../src/test/java/pt/psoft/g1/psoftg1/genremanagement/model/GenreBoundaryTest.java) | 4 | 0 | 0 | 12 | No |
| [GenreRenameTest](../../../../src/test/java/pt/psoft/g1/psoftg1/genremanagement/model/GenreRenameTest.java) | 2 | 0 | 0 | 2 | No |
| [GenreTest](../../../../src/test/java/pt/psoft/g1/psoftg1/genremanagement/model/GenreTest.java) | 4 | 0 | 0 | 4 | No |
| [GenreCrudServiceTest](../../../../src/test/java/pt/psoft/g1/psoftg1/genremanagement/services/GenreCrudServiceTest.java) | 4 | 0 | 0 | 4 | No |
| [GenreLendingsDTOTest](../../../../src/test/java/pt/psoft/g1/psoftg1/genremanagement/services/GenreLendingsDTOTest.java) | 3 | 0 | 0 | 6 | No |
| [GenreServiceImplTest](../../../../src/test/java/pt/psoft/g1/psoftg1/genremanagement/services/GenreServiceImplTest.java) | 6 | 0 | 0 | 9 | No |
| [LendingApiComponentIT](../../../../src/test/java/pt/psoft/g1/psoftg1/lendingmanagement/api/LendingApiComponentIT.java) | 17 | 0 | 0 | 17 | No |
| [LendingControllerTest](../../../../src/test/java/pt/psoft/g1/psoftg1/lendingmanagement/api/LendingControllerTest.java) | 15 | 0 | 0 | 15 | No |
| [LendingViewMapperTest](../../../../src/test/java/pt/psoft/g1/psoftg1/lendingmanagement/api/LendingViewMapperTest.java) | 5 | 0 | 0 | 5 | No |
| [FineTest](../../../../src/test/java/pt/psoft/g1/psoftg1/lendingmanagement/model/FineTest.java) | 6 | 0 | 0 | 6 | No |
| [LendingNumberTest](../../../../src/test/java/pt/psoft/g1/psoftg1/lendingmanagement/model/LendingNumberTest.java) | 12 | 0 | 0 | 12 | No |
| [LendingTest](../../../../src/test/java/pt/psoft/g1/psoftg1/lendingmanagement/model/LendingTest.java) | 31 | 0 | 0 | 31 | No |
| [LendingRepositoryIntegrationIT](../../../../src/test/java/pt/psoft/g1/psoftg1/lendingmanagement/repositories/LendingRepositoryIntegrationIT.java) | 16 | 0 | 0 | 16 | Yes |
| [LendingServiceImplIT](../../../../src/test/java/pt/psoft/g1/psoftg1/lendingmanagement/services/LendingServiceImplIT.java) | 5 | 3 | 3 | 5 | Yes |
| [LendingServiceImplNotificationTest](../../../../src/test/java/pt/psoft/g1/psoftg1/lendingmanagement/services/LendingServiceImplNotificationTest.java) | 10 | 0 | 0 | 10 | No |
| [LendingServiceImplPolicyTest](../../../../src/test/java/pt/psoft/g1/psoftg1/lendingmanagement/services/LendingServiceImplPolicyTest.java) | 11 | 0 | 0 | 11 | No |
| [AdaptiveEligibilityRuleTest](../../../../src/test/java/pt/psoft/g1/psoftg1/lendingmanagement/services/policy/AdaptiveEligibilityRuleTest.java) | 8 | 0 | 0 | 8 | No |
| [AdaptiveLendingPolicyTest](../../../../src/test/java/pt/psoft/g1/psoftg1/lendingmanagement/services/policy/AdaptiveLendingPolicyTest.java) | 8 | 0 | 0 | 8 | No |
| [AdaptivePolicyParametersTest](../../../../src/test/java/pt/psoft/g1/psoftg1/lendingmanagement/services/policy/AdaptivePolicyParametersTest.java) | 9 | 0 | 0 | 9 | No |
| [LendingPolicySelectorTest](../../../../src/test/java/pt/psoft/g1/psoftg1/lendingmanagement/services/policy/LendingPolicySelectorTest.java) | 6 | 0 | 0 | 6 | No |
| [PolicyDecisionAndHistoryTest](../../../../src/test/java/pt/psoft/g1/psoftg1/lendingmanagement/services/policy/PolicyDecisionAndHistoryTest.java) | 8 | 0 | 0 | 8 | No |
| [PolicySelectionParametersTest](../../../../src/test/java/pt/psoft/g1/psoftg1/lendingmanagement/services/policy/PolicySelectionParametersTest.java) | 7 | 0 | 0 | 7 | No |
| [ReaderLendingHistoryProviderTest](../../../../src/test/java/pt/psoft/g1/psoftg1/lendingmanagement/services/policy/ReaderLendingHistoryProviderTest.java) | 6 | 0 | 0 | 6 | No |
| [StandardLendingPolicyTest](../../../../src/test/java/pt/psoft/g1/psoftg1/lendingmanagement/services/policy/StandardLendingPolicyTest.java) | 11 | 0 | 0 | 11 | No |
| [StandardPolicyParametersTest](../../../../src/test/java/pt/psoft/g1/psoftg1/lendingmanagement/services/policy/StandardPolicyParametersTest.java) | 6 | 0 | 0 | 6 | No |
| [EmailNotificationServiceTest](../../../../src/test/java/pt/psoft/g1/psoftg1/notificationmanagement/services/EmailNotificationServiceTest.java) | 5 | 0 | 0 | 5 | No |
| [FourthNotificationChannelExtensionTest](../../../../src/test/java/pt/psoft/g1/psoftg1/notificationmanagement/services/FourthNotificationChannelExtensionTest.java) | 3 | 0 | 0 | 3 | No |
| [NotificationChannelCompositionTest](../../../../src/test/java/pt/psoft/g1/psoftg1/notificationmanagement/services/NotificationChannelCompositionTest.java) | 5 | 0 | 0 | 7 | No |
| [NotificationParametersTest](../../../../src/test/java/pt/psoft/g1/psoftg1/notificationmanagement/services/NotificationParametersTest.java) | 2 | 0 | 0 | 3 | No |
| [SMSNotificationServiceTest](../../../../src/test/java/pt/psoft/g1/psoftg1/notificationmanagement/services/SMSNotificationServiceTest.java) | 6 | 0 | 0 | 11 | No |
| [WebhookNotificationServiceTest](../../../../src/test/java/pt/psoft/g1/psoftg1/notificationmanagement/services/WebhookNotificationServiceTest.java) | 6 | 0 | 0 | 6 | No |
| [NotificationApiComponentIT](../../../../src/test/java/pt/psoft/g1/psoftg1/readermanagement/api/NotificationApiComponentIT.java) | 6 | 0 | 0 | 6 | No |
| [ReaderApiComponentIT](../../../../src/test/java/pt/psoft/g1/psoftg1/readermanagement/api/ReaderApiComponentIT.java) | 5 | 0 | 0 | 5 | No |
| [ReaderControllerBehaviourTest](../../../../src/test/java/pt/psoft/g1/psoftg1/readermanagement/api/ReaderControllerBehaviourTest.java) | 9 | 0 | 0 | 10 | No |
| [ReaderControllerUnitTest](../../../../src/test/java/pt/psoft/g1/psoftg1/readermanagement/api/ReaderControllerUnitTest.java) | 4 | 0 | 0 | 6 | No |
| [ReaderQuoteMappingIT](../../../../src/test/java/pt/psoft/g1/psoftg1/readermanagement/api/ReaderQuoteMappingIT.java) | 1 | 0 | 0 | 1 | No |
| [ReaderSearchPersistenceIT](../../../../src/test/java/pt/psoft/g1/psoftg1/readermanagement/api/ReaderSearchPersistenceIT.java) | 1 | 0 | 0 | 1 | No |
| [BirthDateTest](../../../../src/test/java/pt/psoft/g1/psoftg1/readermanagement/model/BirthDateTest.java) | 4 | 0 | 0 | 4 | No |
| [PhoneNumberTest](../../../../src/test/java/pt/psoft/g1/psoftg1/readermanagement/model/PhoneNumberTest.java) | 4 | 0 | 0 | 4 | No |
| [ReaderDetailsNotificationChannelsTest](../../../../src/test/java/pt/psoft/g1/psoftg1/readermanagement/model/ReaderDetailsNotificationChannelsTest.java) | 10 | 0 | 0 | 18 | No |
| [ReaderDetailsPhoneCollaborationIT](../../../../src/test/java/pt/psoft/g1/psoftg1/readermanagement/model/ReaderDetailsPhoneCollaborationIT.java) | 6 | 0 | 0 | 6 | No |
| [ReaderTest](../../../../src/test/java/pt/psoft/g1/psoftg1/readermanagement/model/ReaderTest.java) | 9 | 0 | 0 | 9 | No |
| [ReaderValueObjectBoundaryTest](../../../../src/test/java/pt/psoft/g1/psoftg1/readermanagement/model/ReaderValueObjectBoundaryTest.java) | 5 | 0 | 0 | 13 | No |
| [NotificationPreferenceServiceTest](../../../../src/test/java/pt/psoft/g1/psoftg1/readermanagement/services/NotificationPreferenceServiceTest.java) | 8 | 0 | 0 | 9 | No |
| [ReaderServiceImplTest](../../../../src/test/java/pt/psoft/g1/psoftg1/readermanagement/services/ReaderServiceImplTest.java) | 13 | 0 | 0 | 13 | No |
| [NameTest](../../../../src/test/java/pt/psoft/g1/psoftg1/shared/model/NameTest.java) | 9 | 0 | 0 | 9 | No |
| [PhotoTest](../../../../src/test/java/pt/psoft/g1/psoftg1/shared/model/PhotoTest.java) | 2 | 0 | 0 | 2 | No |
| [FileStorageServiceTest](../../../../src/test/java/pt/psoft/g1/psoftg1/shared/services/FileStorageServiceTest.java) | 8 | 0 | 0 | 8 | No |
| [EmailNotificationDeliveryIT](../../../../src/test/java/pt/psoft/g1/psoftg1/system/EmailNotificationDeliveryIT.java) | 1 | 0 | 0 | 1 | No |
| [LendingSystemIT](../../../../src/test/java/pt/psoft/g1/psoftg1/system/LendingSystemIT.java) | 9 | 0 | 0 | 9 | No |
| [NotificationSystemIT](../../../../src/test/java/pt/psoft/g1/psoftg1/system/NotificationSystemIT.java) | 3 | 0 | 0 | 3 | No |
| [ReaderGenreSystemIT](../../../../src/test/java/pt/psoft/g1/psoftg1/system/ReaderGenreSystemIT.java) | 2 | 0 | 0 | 2 | Yes |
| [ScheduledNotificationCheckIT](../../../../src/test/java/pt/psoft/g1/psoftg1/system/ScheduledNotificationCheckIT.java) | 1 | 0 | 0 | 1 | No |
| [SmsNotificationDeliveryIT](../../../../src/test/java/pt/psoft/g1/psoftg1/system/SmsNotificationDeliveryIT.java) | 2 | 0 | 0 | 2 | No |
| [WebhookNotificationDeliveryIT](../../../../src/test/java/pt/psoft/g1/psoftg1/system/WebhookNotificationDeliveryIT.java) | 2 | 0 | 0 | 2 | No |
| [ApiIntegrationTestSupport](../../../../src/test/java/pt/psoft/g1/psoftg1/testutils/ApiIntegrationTestSupport.java) | 0 | 0 | 0 | 0 | Yes |
| [JsonHelper](../../../../src/test/java/pt/psoft/g1/psoftg1/testutils/JsonHelper.java) | 0 | 0 | 0 | 0 | No |
| [LocalNotificationReceiver](../../../../src/test/java/pt/psoft/g1/psoftg1/testutils/LocalNotificationReceiver.java) | 0 | 0 | 0 | 0 | No |
| [LocalSmtpReceiver](../../../../src/test/java/pt/psoft/g1/psoftg1/testutils/LocalSmtpReceiver.java) | 0 | 0 | 0 | 0 | No |
| [NotificationDeliveryITSupport](../../../../src/test/java/pt/psoft/g1/psoftg1/testutils/NotificationDeliveryITSupport.java) | 0 | 0 | 0 | 0 | No |
| [UserTestDataFactory](../../../../src/test/java/pt/psoft/g1/psoftg1/testutils/UserTestDataFactory.java) | 0 | 0 | 0 | 0 | No |
| [UserAdminApiIntegrationTest](../../../../src/test/java/pt/psoft/g1/psoftg1/usermanagement/api/UserAdminApiIntegrationTest.java) | 6 | 0 | 0 | 6 | No |
| [UserServiceTest](../../../../src/test/java/pt/psoft/g1/psoftg1/usermanagement/services/UserServiceTest.java) | 8 | 0 | 0 | 8 | No |

A zero executed count is retained rather than hidden; inspect its live declarations and comments before interpreting it as missing execution. See [methodology and reproduction](README.md) and [raw counters](metrics.json).
