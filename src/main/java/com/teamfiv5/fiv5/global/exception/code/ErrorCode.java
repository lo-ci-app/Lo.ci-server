package com.teamfiv5.fiv5.global.exception.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode implements BaseErrorCode {

    // == 기본 에러 ==
    INVALID_REQUEST("COMMON400", "요청이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("COMMON401", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("COMMON403", "접근이 금지되었습니다.", HttpStatus.FORBIDDEN),
    NOT_FOUND("COMMON404", "요청한 자원을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INTERNAL_SERVER_ERROR("COMMON500", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    // == Auth 관련 에러 ==
    APPLE_TOKEN_VERIFY_FAILED("AUTH401_1", "Apple ID 토큰 검증에 실패했습니다.", HttpStatus.UNAUTHORIZED),
    NICKNAME_DUPLICATED("AUTH409_1", "이미 사용 중인 닉네임입니다.", HttpStatus.CONFLICT),

    // == User 관련 에러 ==
    USER_NOT_FOUND("USER404_1", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    BLUETOOTH_TOKEN_NOT_ISSUED("USER404_2", "블루투스 토큰이 발급되지 않았습니다.", HttpStatus.NOT_FOUND),

    // == S3 관련 에러 ==
    FILE_IS_EMPTY("FILE400_1", "업로드할 파일이 비어있습니다.", HttpStatus.BAD_REQUEST),
    S3_UPLOAD_FAILED("S3500_1", "S3 파일 업로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),


    // == 친구 관련 에러 ==
    SELF_FRIEND_REQUEST("FRIEND400_1", "자기 자신에게 친구 요청을 보낼 수 없습니다.", HttpStatus.BAD_REQUEST),
    FRIEND_REQUEST_ALREADY_EXISTS("FRIEND409_1", "이미 친구 관계이거나 요청 대기 중입니다.", HttpStatus.CONFLICT),
    FRIEND_LIMIT_EXCEEDED("FRIEND409_2", "친구는 최대 5명까지 추가할 수 있습니다.", HttpStatus.CONFLICT),
    TARGET_FRIEND_LIMIT_EXCEEDED("FRIEND409_3", "상대방의 친구 수가 꽉 찼습니다.", HttpStatus.CONFLICT),
    FRIEND_REQUEST_NOT_FOUND("FRIEND404_1", "존재하지 않는 친구 요청입니다.", HttpStatus.NOT_FOUND),
    INVALID_TARGET_TOKEN("FRIEND404_2", "유효하지 않은 토큰이거나 만료된 사용자입니다.", HttpStatus.NOT_FOUND),
    NOT_FRIENDS("FRIEND404_3", "두 사용자 간에 친구 관계가 존재하지 않습니다.", HttpStatus.NOT_FOUND),

    // == 포스트 관련 에러 ==
    POST_NOT_FOUND("POST404_1", "게시물을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    NOT_POST_AUTHOR("POST403_1", "게시물에 대한 권한이 없습니다.", HttpStatus.FORBIDDEN),

    // == 방명록 관련 에러 ==
    SELF_GUESTBOOK_ENTRY("GUESTBOOK400_1", "자신의 방명록에 글을 쓸 수 없습니다.", HttpStatus.BAD_REQUEST),
    GUESTBOOK_ENTRY_NOT_FOUND("GUESTBOOK404_1", "방명록을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);


    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}