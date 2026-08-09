# System Data Transfer Objects (DTOs)

This document lists all DTOs used across the different modules of the SwipeLab backend, including shared/common DTOs.

## Analytics Module DTOs

### DashboardStatsResponse
**Description**: Data Transfer Object for DashboardStatsResponse

**Fields**:
- `long totalUsers` 
- `long totalSwipes` 
- `long activeTasks` 
- `long totalImages` 

### ExportRequest
**Description**: Data Transfer Object for ExportRequest

**Fields**:
- `List<Long> taskIds` 

### PerformanceBreakdownResponse
**Description**: Data Transfer Object for PerformanceBreakdownResponse

**Fields**:
- `List<CategoryStat> byCategory` 
- `String category` 
- `Double accuracy` 
- `Integer total` 

### PlatformOverviewResponse
**Description**: Data Transfer Object for PlatformOverviewResponse

**Fields**:
- `ActivitySummary today` 
- `ActivitySummary thisWeek` 
- `ActivitySummary thisMonth` 
- `List<ConfidenceTrendPoint> confidenceTrend` 
- `List<LabelDistributionPoint> labelDistribution` 
- `PlatformTotals totals` 
- `long classifications` 
- `long uniqueImages` 
- `long uniqueUsers` 
- `long uniqueTasks` 
- `long uniqueExperiments` 
- `String date` 
- `double averageCredibility` 
- `long classificationCount` 
- `String label` 
- `long count` 
- `double percentage` 
- `long totalUsers` 
- `long totalClassifications` 
- `long totalImages` 
- `long activeTasks` 

### TaskAnalyticsResponse
**Description**: Data Transfer Object for TaskAnalyticsResponse

**Fields**:
- `Long taskId` 
- `String status` 
- `Progress progress` 
- `Consensus consensus` 
- `List<SpeciesAnalytics> speciesAnalytics` 
- `Participation participation` 
- `Quality quality` 
- `List<TimeSeriesPoint> timeSeries` 
- `String generatedAt` 
- `Integer totalImages` 
- `Integer imagesClassified` 
- `Integer completedImages` 
- `Double percentComplete` 
- `Double overallAverage` 
- `Integer lowConsensusImages` 
- `Double threshold` 
- `String name` 
- `Integer classificationCount` 
- `Double agreementRate` 
- `ConfusionMatrix confusionMatrix` 
- `Integer truePositive` 
- `Integer falsePositive` 
- `Integer falseNegative` 
- `Integer trueNegative` 
- `Integer activeUsers` 
- `Integer totalClassifications` 
- `Integer averageClassificationsPerUser` 
- `Long medianResponseTimeMs` 
- `Double averageCredibility` 
- `Double expertAgreement` 
- `Integer lowQualityUsers` 
- `String date` 
- `Integer classifications` 
- `Integer consensusReached` 

### TimeSeriesResponse
**Description**: Data Transfer Object for TimeSeriesResponse

**Fields**:
- `String metric` 
- `List<Point> points` 
- `String timestamp` 
- `Double value` 

### UserPerformanceResponse
**Description**: Data Transfer Object for UserPerformanceResponse

**Fields**:
- `String username` 
- `String displayName` 
- `int totalClassifications` 
- `int goldImageClassifications` 
- `int correctGoldClassifications` 
- `double goldAccuracy` 
- `double credibilityScore` 
- `int currentStreak` 
- `long points` 

### UserProgressResponse
**Description**: Data Transfer Object for UserProgressResponse

**Fields**:
- `Integer completed` 
- `Double accuracy` 

### UserStatisticsResponse
**Description**: Data Transfer Object for UserStatisticsResponse

**Fields**:
- `Summary summary` 
- `Trend trend` 
- `Integer totalClassifications` 
- `Integer correctClassifications` 
- `Double accuracy` 
- `Double contributionPercentage` 
- `Rank rank` 
- `Integer rankPercentile` 
- `Integer currentStreak` 
- `Integer longestStreak` 
- `Integer daily` 
- `Integer weekly` 
- `Integer monthly` 
- `Integer allTime` 
- `List<DayPoint> byDay` 
- `String date` 
- `Double accuracy` 

### UserVsExpertsResponse
**Description**: Data Transfer Object for UserVsExpertsResponse

**Fields**:
- `Stat user` 
- `Stat experts` 
- `Stat difference` 
- `Double accuracy` 

### UserVsUsersResponse
**Description**: Data Transfer Object for UserVsUsersResponse

**Fields**:
- `Double percentile` 
- `Double averageUserAccuracy` 

## Auth Module DTOs

### AuthResponse
**Description**: Data Transfer Object for AuthResponse

*(No explicit private fields found, may inherit or be empty)*

### EmailVerificationRequest
**Description**: Data Transfer Object for EmailVerificationRequest

**Fields**:
- `String token` 

### ExternalLoginRequest
**Description**: Carries the full Stardbi login response so the backend can validate the token AND provision a local user if needed.

**Fields**:
- `String access` 
- `String refresh` 
- `Integer lifetime` 
- `Long id` 
- `String username` 
- `String firstName` 
- `String lastName` 
- `String email` 

### ForgotPasswordRequest
**Description**: Data Transfer Object for ForgotPasswordRequest

**Fields**:
- `String email` 

### InviteAdminRequest
**Description**: Data Transfer Object for InviteAdminRequest

**Fields**:
- `String email` 

### LoginRequest
**Description**: Data Transfer Object for LoginRequest

**Fields**:
- `String username` 
- `String password` 

### RegisterRequest
**Description**: Data Transfer Object for RegisterRequest

**Fields**:
- `String username` 
- `String email` 
- `String password` 
- `String displayName` 

### ResetPasswordRequest
**Description**: Data Transfer Object for ResetPasswordRequest

**Fields**:
- `String token` 
- `String newPassword` 

### SuspiciousActivityResponse
**Description**: API response for a single SuspiciousActivityRecord. Exposed on GET /api/admin/suspicious-activity.

**Fields**:
- `Long id` 
- `String username` 
- `String reason` 
- `Long responseTimeMs` 
- `Long taskId` 
- `WarningLevel severity` 
- `LocalDateTime createdAt` 

## Classification Module DTOs

### BatchImageDto
**Description**: Data Transfer Object for BatchImageDto

**Fields**:
- `Long imageId` 
- `Long taskId` 
- `String question` 
- `ImageDataDto image` 
- `List<ReferenceImageDto> referenceImages` 

### ClassificationRequest
**Description**: Data Transfer Object for ClassificationRequest

**Fields**:
- `Long imageId` 
- `Long responseTimeMs` 

### ClassificationWarningDto
**Description**: Data Transfer Object for ClassificationWarningDto

**Fields**:
- `String level` 
- `String message` 
- `int strikeCount` 
- `int strikesUntilBan` 

### GoldImageRequest
**Description**: Data Transfer Object for GoldImageRequest

**Fields**:
- `Long imageId` 
- `String species` 

### GoldImageResponse
**Description**: Data Transfer Object for GoldImageResponse

**Fields**:
- `Long id` 
- `Long imageId` 
- `String species` 
- `String imageUrl` 

### ImageBatchResponse
**Description**: Data Transfer Object for ImageBatchResponse

**Fields**:
- `List<ImageResponse> images` 
- `Long nextCursor` 

### ImageDataDto
**Description**: Data Transfer Object for ImageDataDto

**Fields**:
- `String contentType` 
- `String data` 

### ImageResponse
**Description**: Data Transfer Object for ImageResponse

**Fields**:
- `Long id` 
- `String imageUrl` 
- `String thumbnailUrl` 
- `String caption` 
- `Long taskId` 
- `Integer priority` 
- `Boolean isGoldStandard` 

### ImageUploadRequest
**Description**: Data Transfer Object for ImageUploadRequest

**Fields**:
- `String imageUrl` 
- `String caption` 
- `Long taskId` 
- `Long correctLabelId` 

### NextBatchResponse
**Description**: Data Transfer Object for NextBatchResponse

**Fields**:
- `List<BatchImageDto> images` 
- `ClassificationWarningDto warning` 

### ReferenceImageDto
**Description**: Data Transfer Object for ReferenceImageDto

**Fields**:
- `String imageUrl` 
- `String contentType` 
- `String data` 
- `String caption` 

### SpeciesReferenceImageDto
**Description**: Data Transfer Object for SpeciesReferenceImageDto

**Fields**:
- `Long id` 
- `Long labelId` 
- `String imageUrl` 
- `String thumbnailUrl` 
- `Long fileSizeBytes` 
- `String caption` 
- `String uploadedBy` 
- `LocalDateTime createdAt` 

### SubmitClassificationRequest
**Description**: Data Transfer Object for SubmitClassificationRequest

**Fields**:
- `Long imageId` 
- `Long taskId` 
- `String question` 
- `Long responseTimeMs` 

### UserClassification
**Description**: Data Transfer Object for UserClassification

**Fields**:
- `Long imageId` 

## Collection Module DTOs

### CollectionEntryResponse
**Description**: Data Transfer Object for CollectionEntryResponse

**Fields**:
- `Long id` 
- `Long imageId` 
- `String species` 
- `String imageUrl` 
- `Long taskId` 
- `LocalDateTime taggedAt` 

### CollectionStatsResponse
**Description**: Data Transfer Object for CollectionStatsResponse

**Fields**:
- `long total` 

## Shared / Common DTOs

### ClassificationResponse
**Description**: Data Transfer Object for ClassificationResponse

**Fields**:
- `Long id` 
- `String userId` 
- `Long imageId` 
- `Long labelId` 
- `Boolean isCorrect` 
- `LocalDateTime createdAt` 

### ErrorResponse
**Description**: Machine-readable error code for frontend branching. Examples: "ACCOUNT_BANNED", "ACCESS_DENIED" Null for errors that don't require special client handling.

**Fields**:
- `int status` 
- `String error` 
- `String message` 
- `String path` 
- `String errorCode` 

### LeaderboardResponse
**Description**: Data Transfer Object for LeaderboardResponse

*(No explicit private fields found, may inherit or be empty)*

### MyTaskListResponse
**Description**: Data Transfer Object for MyTaskListResponse

**Fields**:
- `Integer page` 
- `Integer pageSize` 
- `Integer totalPages` 
- `Integer totalTasks` 
- `List<UserTaskSummary> tasks` 
- `Long taskId` 
- `String name` 
- `String description` 
- `Integer totalImages` 
- `Integer imagesClassified` 
- `List<TargetSpeciesResponse> species` 

### PlayTaskResponse
**Description**: Data Transfer Object for PlayTaskResponse

**Fields**:
- `Long taskId` 
- `List<SpeciesRefDto> species` 
- `List<ImageToClassifyDto> images` 
- `String scientificName` 
- `String commonName` 
- `List<RefImageDto> referenceImages` 
- `String imageUrl` 
- `String caption` 
- `Long imageId` 
- `String imageBuffer` 
- `String contentType` 
- `String question` 
- `Long taskId` 
- `String species` 

### RecipientGroupListResponse
**Description**: Data Transfer Object for RecipientGroupListResponse

**Fields**:
- `Integer page` 
- `Integer pageSize` 
- `Integer totalPages` 
- `Integer totalGroups` 
- `List<RecipientGroupResponse> recipientGroups` 

### StatsResponse
**Description**: Data Transfer Object for StatsResponse

*(No explicit private fields found, may inherit or be empty)*

### TaskAnalyticsResponse
**Description**: Data Transfer Object for TaskAnalyticsResponse

**Fields**:
- `Long taskId` 
- `String taskName` 
- `String status` 
- `int totalImages` 
- `int classifiedImages` 
- `double completionPercentage` 
- `double averageConsensus` 
- `int lowConsensusCount` 
- `int highConsensusCount` 
- `int totalClassifications` 
- `int uniqueClassifiers` 

## Gamification Module DTOs

### BadgeDto
**Description**: Data Transfer Object for BadgeDto

**Fields**:
- `String title` 
- `String iconUrl` 

### ChallengeDto
**Description**: Data Transfer Object for ChallengeDto

**Fields**:
- `UUID challengeId` 
- `String name` 
- `String description` 
- `int progress` 
- `int target` 
- `boolean completed` 
- `LocalDateTime windowStart` 
- `LocalDateTime windowEnd` 
- `BadgeDto badge` 

### GamificationUserInfoResponse
**Description**: Data Transfer Object for GamificationUserInfoResponse

**Fields**:
- `long score` 
- `String badge` 
- `int currentStreak` 

### RankResponse
**Description**: Response payload for GET /api/v1/gamification/rank. Carries everything the frontend needs to render a rank badge and progress bar.

**Fields**:
- `String tier` 
- `int yesTagCount` 
- `int nextTierAt` 
- `int progressPercent` 

### UserBadgeDto
**Description**: Data Transfer Object for UserBadgeDto

**Fields**:
- `String title` 
- `String description` 
- `String iconUrl` 
- `LocalDateTime earnedAt` 

## Recipients Module DTOs

### CreateRecipientGroupRequest
**Description**: Data Transfer Object for CreateRecipientGroupRequest

**Fields**:
- `String name` 
- `List<String> usernames` 

### RecipientGroupResponse
**Description**: Data Transfer Object for RecipientGroupResponse

**Fields**:
- `Long groupId` 
- `String name` 
- `Integer userCount` 
- `List<String> usernames` 
- `OffsetDateTime createdAt` 
- `OffsetDateTime updatedAt` 

### UpdateRecipientGroupRequest
**Description**: Data Transfer Object for UpdateRecipientGroupRequest

**Fields**:
- `List<String> addUsernames` 
- `List<String> removeUsernames` 

## Tasks Module DTOs

### CreateTaskRequest
**Description**: Map of Species Name -> List of SpeciesReferenceImage IDs Selected from the species image pool for this specific task.

**Fields**:
- `String name` 
- `String description` 
- `List<TargetSpeciesRequest> targetSpecies` 
- `List<Long> experiments` 
- `List<Long> recipientGroups` 
- `List<String> assignedUsernames` 
- `List<String> sharedWithResearchers` 
- `Boolean isPublic` 
- `int minClassificationsPerImage` 
- `double consensusThreshold` 

### ReferenceImageRequest
**Description**: Data Transfer Object for ReferenceImageRequest

**Fields**:
- `String contentType` 
- `String data` 
- `String caption` 

### ReferenceImageResponse
**Description**: Data Transfer Object for ReferenceImageResponse

**Fields**:
- `String contentType` 
- `String data` 
- `String caption` 
- `String imageUrl` 

### TargetSpeciesRequest
**Description**: Scientific name (e.g. "Vespa mandarinia")

**Fields**:
- `String name` 
- `List<ReferenceImageRequest> referenceImages` 

### TargetSpeciesResponse
**Description**: Scientific name Example: "Vespa mandarinia"

**Fields**:
- `String name` 
- `String commonName` 
- `List<ReferenceImageResponse> referenceImages` 

### TaskPageResponse
**Description**: Data Transfer Object for TaskPageResponse

**Fields**:
- `int page` 
- `int pageSize` 
- `int totalPages` 
- `long totalTasks` 
- `List<TaskResponse> tasks` 

### TaskProgressResponse
**Description**: Data Transfer Object for TaskProgressResponse

**Fields**:
- `int totalImages` 
- `int imagesClassified` 

### TaskResponse
**Description**: Data Transfer Object for TaskResponse

**Fields**:
- `Long taskId` 
- `String status` 
- `String name` 
- `String description` 
- `List<TargetSpeciesResponse> targetSpecies` 
- `List<Long> experiments` 
- `List<Long> recipientGroups` 
- `TaskProgressResponse progress` 
- `boolean assignedToUser` 
- `OffsetDateTime createdAt` 
- `OffsetDateTime deadline` 
- `Integer minClassificationsPerImage` 
- `Double consensusThreshold` 
- `Boolean isPublic` 
- `List<String> assignedUsernames` 
- `List<String> sharedWithResearchers` 

### UpdateTaskRequest
**Description**: Map of Species Name -> List of SpeciesReferenceImage IDs Selected from the species image pool for this specific task.

**Fields**:
- `String name` 
- `String description` 
- `List<TargetSpeciesRequest> targetSpecies` 
- `List<Long> experiments` 
- `List<Long> recipientGroups` 
- `List<String> assignedUsernames` 
- `List<String> sharedWithResearchers` 
- `Boolean isPublic` 

## Users Module DTOs

### AdminNotificationResponse
**Description**: API response for a single AdminNotification record. Exposed on GET /api/admin/notifications.

**Fields**:
- `Long id` 
- `NotificationType type` 
- `NotificationSeverity severity` 
- `String title` 
- `String message` 
- `String targetUsername` 
- `Boolean isRead` 
- `LocalDateTime createdAt` 

### UpdateProfileRequest
**Description**: Data Transfer Object for UpdateProfileRequest

**Fields**:
- `String displayName` 
- `String profileImageUrl` 

### UserProfileResponse
**Description**: Data Transfer Object for UserProfileResponse

**Fields**:
- `String username` 
- `String email` 
- `String displayName` 
- `String profileImageUrl` 
- `UserRole role` 
- `Long score` 
- `List<String> badges` 
- `String rank` 
- `int currentStreak` 
- `boolean isSuperAdmin` 
- `boolean active` 
- `Double credibilityScore` 


