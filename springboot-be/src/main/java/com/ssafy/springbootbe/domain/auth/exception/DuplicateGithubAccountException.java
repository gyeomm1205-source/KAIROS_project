package com.ssafy.springbootbe.domain.auth.exception;

public class DuplicateGithubAccountException extends RuntimeException {

    public DuplicateGithubAccountException(String githubLogin) {
        super("이미 가입된 GitHub 계정입니다. login=" + githubLogin);
    }
}
