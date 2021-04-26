#pragma once

typedef enum
{
    EXPOSURE_NO_DETECT = 0,
    EXPOSURE_DETECT = 1,
    EXPOSURE_UPDATE_FAILED = 2,
    EXPOSURE_NO_UPDATE = 3, //No update happened yet
} exposure_status;

struct ContactInformation
{
    int enin;
    char rpis[][16];
};