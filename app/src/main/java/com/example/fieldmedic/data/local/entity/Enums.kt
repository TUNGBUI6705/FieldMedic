package com.example.fieldmedic.data.local.entity

enum class SyncStatus { SYNCED, PENDING, SENDING, ERROR, CONFLICT }
enum class Gender { MALE, FEMALE, OTHER }
enum class AllergyStatus { UNKNOWN, NONE_KNOWN, HAS_ALLERGY }
enum class AccessPolicy { STATION, RESTRICTED }
enum class StaffRole { NURSE, SUPERVISOR, CONSULTANT }
enum class Safety { SAFE, CAUTION, AVOID, UNKNOWN }
enum class Population { ADULT, PREGNANT, CHILD, INFANT }
enum class AbnormalLevel { NORMAL, WARNING, CRITICAL }
enum class AlertLevel { NONE, WARNING, CRITICAL }
enum class VitalType { BP_SYS, BP_DIA, PULSE, TEMP, SPO2, RESP_RATE, WEIGHT, HEIGHT, GLUCOSE }
enum class VitalSource { BLE, MANUAL, BLE_EDITED }
enum class VitalContext { FASTING, POSTPRANDIAL, RANDOM }
enum class BloodTypeSource { SELF_REPORTED, LAB_CONFIRMED }
enum class AllergySeverity { MILD, MODERATE, SEVERE }
enum class AllergyRecordStatus { ACTIVE, RESOLVED, ENTERED_IN_ERROR }
enum class ConditionStatus { ACTIVE, RESOLVED }
enum class MedStatus { ACTIVE, STOPPED }
enum class MedSource { PRESCRIBED_HERE, REPORTED }
enum class AppointmentStatus { SCHEDULED, DONE, MISSED, CANCELLED }
enum class EncounterStatus { DRAFT, COMPLETED, CANCELLED }
enum class PregnancyStatus { NOT_APPLICABLE, NOT_PREGNANT, PREGNANT, BREASTFEEDING, UNKNOWN }
enum class ConsentBy { PATIENT, GUARDIAN }
enum class AllergyCheck { PASSED, OVERRIDDEN, NOT_CHECKED }
enum class PregnancyCheck { OK, WARNED, NOT_APPLICABLE }
enum class ReferralUrgency { EMERGENCY, URGENT, ROUTINE }
enum class ReferralStatus { ISSUED, RECEIVED, COMPLETED, CANCELLED }
enum class AddendumKind { ADDITION, CORRECTION }
enum class PhotoOwnerType { PATIENT_AVATAR, ENCOUNTER }
enum class PhotoUploadStatus { LOCAL, UPLOADING, UPLOADED, FAILED }
enum class CommentTarget { RECORD, ENCOUNTER, VITAL, PHOTO }
enum class CommentType { NOTE, QUESTION, ADVICE, REFERRAL_FEEDBACK }
enum class RecordRole { OWNER, EDITOR, COMMENTER, VIEWER }
enum class MemberSource { OWNER, INVITE }
enum class MemberStatus { ACTIVE, REVOKED, EXPIRED }
enum class InviteStatus { ACTIVE, REDEEMED, EXPIRED, REVOKED }
enum class QueueOperation { CREATE, UPDATE, DELETE }
enum class QueueStatus { PENDING, IN_FLIGHT, ERROR, BLOCKED }
enum class SyncErrorCategory { NETWORK, SERVER_BUSY, INVALID_DATA, AUTH_EXPIRED, PERMISSION, PHOTO }
enum class ConflictStatus { OPEN, ESCALATED, RESOLVED }
enum class SyncEntityType {
    PATIENT, PATIENT_ALLERGY, PATIENT_CONDITION, PATIENT_MEDICATION, APPOINTMENT,
    ENCOUNTER, VITAL_SIGN, MEDICATION_GIVEN, REFERRAL, ENCOUNTER_ADDENDUM, PHOTO,
    RECORD_COMMENT, RECORD_COMMENT_REVISION, RECORD_LOCK, AUDIT_LOG
}
enum class DeviceType { BP, THERMOMETER }
enum class LogLevel { DEBUG, INFO, WARN, ERROR }
enum class AuditResult { OK, DENIED, FAIL }
enum class AuditAction {
    VIEW_RECORD, REVEAL_NATIONAL_ID, EXPORT_REFERRAL, LOCK_ENABLE, LOCK_DISABLE, UNLOCK_OK,
    UNLOCK_FAIL, PASSWORD_CHANGE, SHARE_CREATE, SHARE_REDEEM, SHARE_REVOKE, COMMENT_ADD,
    COMMENT_EDIT, COMMENT_DELETE, CONFLICT_RESOLVE, BREAK_GLASS
}
