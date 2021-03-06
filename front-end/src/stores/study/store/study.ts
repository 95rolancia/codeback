import axios, { AxiosError } from 'axios';
import { makeAutoObservable } from 'mobx';
import { FAIL_TO_CREATE_STUDY, FAIL_TO_LEAVE_STUDY, NOT_EXIST_STUDY } from 'common/string-template';
import { handleServerError } from 'util/http-error';
import studyRepository from '../repository/study-repository';

export interface Study {
  studyId: string | undefined;
  getStudyId: () => Promise<string | undefined>;
  leaveStudy: () => Promise<void>;
  verifyStudy: (d: string) => Promise<void>;
}

export class StudyImpl implements Study {
  studyId = undefined;

  constructor() {
    makeAutoObservable(this);
  }

  async getStudyId() {
    try {
      const res = await studyRepository.getStudyId();
      return res.data;
    } catch (error) {
      if (axios.isAxiosError(error)) {
        const axiosError = error as AxiosError;
        console.log(axiosError.response);
        handleServerError(axiosError);
        throw new Error(FAIL_TO_CREATE_STUDY);
      }
    }
  }

  async leaveStudy() {
    try {
      await studyRepository.leaveStudy();
    } catch (error) {
      if (axios.isAxiosError(error)) {
        const axiosError = error as AxiosError;
        console.log(axiosError.response);
        handleServerError(axiosError);
        throw new Error(FAIL_TO_LEAVE_STUDY);
      }
    }
  }

  async verifyStudy(id: string) {
    try {
      await studyRepository.verifyStudy(id);
    } catch (error) {
      if (axios.isAxiosError(error)) {
        const axiosError = error as AxiosError;
        console.log(axiosError.response);
        handleServerError(axiosError);
        throw new Error(NOT_EXIST_STUDY);
      }
    }
  }
}
