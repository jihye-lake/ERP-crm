package com.nexora.erp.service;

import java.util.List;

/**
 * 엑셀 업로드 결과.
 * 오류가 한 건이라도 있으면 savedCount = 0 (전체 미반영) 이고 errors 에 행별 사유가 들어감.
 */
public record ExcelImportResult(int savedCount, List<RowError> errors) {

    public record RowError(int rowNumber, String projectCode, String reason) {
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public static ExcelImportResult success(int savedCount) {
        return new ExcelImportResult(savedCount, List.of());
    }

    public static ExcelImportResult failed(List<RowError> errors) {
        return new ExcelImportResult(0, List.copyOf(errors));
    }
}
